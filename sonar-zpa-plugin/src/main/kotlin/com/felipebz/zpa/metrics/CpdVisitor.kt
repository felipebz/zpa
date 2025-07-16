/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa.metrics

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.cpd.NewCpdTokens
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck

class CpdVisitor(context: SensorContext, inputFile: InputFile) : PlSqlCheck() {

    private val newCpdTokens: NewCpdTokens = context.newCpdTokens().onFile(inputFile)

    override fun init() {
        subscribeTo(PlSqlGrammar.ANONYMOUS_BLOCK,
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION,
                PlSqlGrammar.CREATE_PACKAGE_BODY,
                PlSqlGrammar.CREATE_TYPE_BODY)
    }

    override fun visitNode(node: AstNode) {
        for (token in node.tokens) {
            saveCpdTokens(token)
        }
    }

    private fun saveCpdTokens(token: Token) {
        newCpdTokens.addToken(token.line, token.column, token.endLine, token.endColumn, token.value)
    }

    override fun leaveFile(node: AstNode) {
        newCpdTokens.save()
    }

}
