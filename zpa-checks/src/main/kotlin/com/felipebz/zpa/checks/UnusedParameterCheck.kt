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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.AstNodeType
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.*
import com.felipebz.zpa.api.semantics.UsageEvidence
import java.util.regex.Pattern

@Rule(priority = Priority.MAJOR, tags = [Tags.UNUSED])
@ConstantRemediation("30min")
@RuleInfo(scope = RuleInfo.Scope.ALL)
@ActivatedByDefault
@OptIn(ZpaExperimentalApi::class)
class UnusedParameterCheck : AbstractBaseCheck() {

    @RuleProperty(key = "ignoreMethods", defaultValue = "")
    var ignoreMethods = ""

    private val ignoreRegex: Pattern? by lazy {
        if ("" != ignoreMethods) {
            Pattern.compile(ignoreMethods, Pattern.CASE_INSENSITIVE)
        } else null
    }

    override fun init() {
        subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_PARAMETER_DECLARATION)
    }

    override fun visitNode(node: AstNode) {
        val owner = node.getFirstAncestorOrNull(*OWNERS) ?: return
        if (isExempt(owner)) return
        val name = node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)
        // SELF parameter in type members
        if (owner.getFirstAncestorOrNull(*SCOPES)?.type == PlSqlGrammar.CREATE_TYPE_BODY && name.tokenValue.equals("self", ignoreCase = true)) {
            return
        }
        // Only a proven absence of uses is reported: an unknown evidence (or none at all) never is.
        if (context.usageEvidence(node) != UsageEvidence.Unused) return
        addIssue(node, getLocalizedMessage(), name.tokenValue)
    }

    /** The rules that exclude every parameter of [owner], the subprogram or cursor that declares them. */
    private fun isExempt(owner: AstNode): Boolean {
        // is overriding something?
        if (PlSqlStructure.isOverridingMember(owner)) return true

        // ignore procedure/function specification and overriding members
        if (owner.type in DECLARATION_OR_CONSTRUCTOR && !owner.hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION)) return true

        // standalone function implemented elsewhere (AGGREGATE USING or PIPELINED ... USING)
        if (owner.type == PlSqlGrammar.CREATE_FUNCTION &&
            !owner.hasDirectChildren(PlSqlGrammar.STATEMENTS_SECTION, PlSqlGrammar.CALL_SPECIFICATION)) return true

        // a SQL macro's parameters are substituted by name into the text it returns
        if (owner.hasDirectChildren(PlSqlGrammar.SQL_MACRO_CLAUSE)) return true

        // the body of an MLE call specification is opaque source code that uses the parameters by name
        if (owner.children.firstOrNull { it.type == PlSqlGrammar.CALL_SPECIFICATION }
                ?.hasDirectChildren(PlSqlGrammar.MLE_DECLARATION) == true) return true

        // cursor declaration (without implementation)
        if (owner.type == PlSqlGrammar.CURSOR_DECLARATION && !owner.hasDirectChildren(DmlGrammar.SELECT_EXPRESSION)) return true

        // ignore methods by name
        val regex = ignoreRegex
        if (owner.type in PROCEDURE_OR_FUNCTION && regex != null) {
            val identifier = owner.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME, PlSqlGrammar.UNIT_NAME)
            if (regex.matcher(identifier.tokenValue).matches()) return true
        }
        return false
    }

    companion object {
        private val OWNERS = arrayOf<AstNodeType>(
            PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.TYPE_CONSTRUCTOR, PlSqlGrammar.CURSOR_DECLARATION
        )

        private val SCOPES = arrayOf<AstNodeType>(
            PlSqlGrammar.CREATE_PROCEDURE, PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.FUNCTION_DECLARATION, PlSqlGrammar.CREATE_PACKAGE, PlSqlGrammar.CREATE_PACKAGE_BODY,
            PlSqlGrammar.SIMPLE_DML_TRIGGER, PlSqlGrammar.INSTEAD_OF_DML_TRIGGER, PlSqlGrammar.COMPOUND_DML_TRIGGER,
            PlSqlGrammar.SYSTEM_TRIGGER, PlSqlGrammar.TYPE_CONSTRUCTOR, PlSqlGrammar.CREATE_TYPE, PlSqlGrammar.CREATE_TYPE_BODY,
            PlSqlGrammar.BLOCK_STATEMENT, PlSqlGrammar.FOR_STATEMENT, PlSqlGrammar.CURSOR_DECLARATION, PlSqlGrammar.FORALL_STATEMENT,
            DmlGrammar.SELECT_EXPRESSION
        )

        val DECLARATION_OR_CONSTRUCTOR =
            arrayOf(PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,
                PlSqlGrammar.TYPE_CONSTRUCTOR)
        val PROCEDURE_OR_FUNCTION =
            arrayOf(PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION)
    }

}
