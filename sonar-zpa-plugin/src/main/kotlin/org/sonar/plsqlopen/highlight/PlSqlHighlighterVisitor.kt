/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.highlight

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.highlighting.NewHighlighting
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck

class PlSqlHighlighterVisitor(context: SensorContext, inputFile: InputFile) : PlSqlCheck() {

    private val highlighting: NewHighlighting = context.newHighlighting().onFile(inputFile)

    override fun visitToken(token: Token) {
        if (token.type is PlSqlTokenType) {
            highlight(token, TypeOfText.STRING)
        }
        if (token.type is PlSqlKeyword) {
            highlight(token, TypeOfText.KEYWORD)
        }
    }

    override fun visitComment(trivia: Trivia, content: String) {
        val token = trivia.token
        if (token.value.startsWith("/**")) {
            highlight(token, TypeOfText.STRUCTURED_COMMENT)
        } else {
            highlight(token, TypeOfText.COMMENT)
        }
    }

    override fun leaveFile(node: AstNode) {
        highlighting.save()
    }

    private fun highlight(token: Token, code: TypeOfText) {
        highlighting.highlight(token.line, token.column, token.endLine, token.endColumn, code)
    }

}
