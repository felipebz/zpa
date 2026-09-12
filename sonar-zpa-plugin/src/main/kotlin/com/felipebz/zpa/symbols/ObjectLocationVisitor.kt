/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.squid.SonarQubePlSqlFile

/**
 * Collects the small amount of source metadata needed by the utPLSQL and coverage importers.
 * The visitor itself is short-lived; only [MappedObject] values escape the AST walk.
 */
internal class ObjectLocationVisitor(
    private val plSqlFile: SonarQubePlSqlFile,
    private val objectLocations: MutableCollection<MappedObject>
) : PlSqlCheck() {

    override fun init() {
        subscribeTo(
            PlSqlGrammar.CREATE_PACKAGE,
            PlSqlGrammar.CREATE_PACKAGE_BODY,
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.CREATE_TYPE,
            PlSqlGrammar.CREATE_TYPE_BODY,
            PlSqlGrammar.SIMPLE_DML_TRIGGER,
            PlSqlGrammar.INSTEAD_OF_DML_TRIGGER,
            PlSqlGrammar.COMPOUND_DML_TRIGGER,
            PlSqlGrammar.SYSTEM_TRIGGER
        )
    }

    override fun visitNode(node: AstNode) {
        val isTriggerBody = node.type in triggerBodyTypes
        val declaration = if (isTriggerBody) node.parentOrNull else node
        val identifier = declaration?.getFirstChildOrNull(
            PlSqlGrammar.IDENTIFIER_NAME,
            PlSqlGrammar.UNIT_NAME
        )?.tokenValue ?: return
        val firstToken = node.tokenOrNull ?: return
        val lastToken = node.lastTokenOrNull ?: return
        val objectType = if (isTriggerBody) PlSqlGrammar.CREATE_TRIGGER else node.type

        objectLocations.add(
            MappedObject(
                identifier,
                objectType,
                plSqlFile.type(),
                plSqlFile.path(),
                plSqlFile.inputFile,
                firstToken.line,
                lastToken.line
            )
        )
    }

    private companion object {
        val triggerBodyTypes = setOf(
            PlSqlGrammar.SIMPLE_DML_TRIGGER,
            PlSqlGrammar.INSTEAD_OF_DML_TRIGGER,
            PlSqlGrammar.COMPOUND_DML_TRIGGER,
            PlSqlGrammar.SYSTEM_TRIGGER
        )
    }
}
