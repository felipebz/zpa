//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.highlight

import com.felipebz.flr.api.Token
import com.felipebz.flr.api.Trivia
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlTokenType
import com.felipebz.zpa.api.checks.PlSqlCheck
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.highlighting.NewHighlighting
import org.sonar.api.batch.sensor.highlighting.TypeOfText

class PlSqlHighlighterVisitor(context: SensorContext, inputFile: InputFile) : PlSqlCheck() {

    private val highlighting: NewHighlighting = context.newHighlighting().onFile(inputFile)

    override fun visitToken(token: Token) {
        val type = token.type
        if (type is PlSqlTokenType) {
            textOf(type)?.let { highlight(token, it) }
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

    private fun textOf(type: PlSqlTokenType): TypeOfText? = when (type) {
        PlSqlTokenType.INTEGER_LITERAL,
        PlSqlTokenType.NUMBER_LITERAL,
        PlSqlTokenType.DATE_LITERAL,
        PlSqlTokenType.TIMESTAMP_LITERAL,
        PlSqlTokenType.TIME_LITERAL -> TypeOfText.CONSTANT
        PlSqlTokenType.STRING_LITERAL,
        PlSqlTokenType.MLE_INLINE_SOURCE,
        PlSqlTokenType.MLE_MODULE_SOURCE,
        PlSqlTokenType.WRAPPED_SOURCE,
        PlSqlTokenType.GRAPHQL_DUALITY_SOURCE -> TypeOfText.STRING
        PlSqlTokenType.CONDITIONAL_BRANCH_BOUNDARY,
        PlSqlTokenType.CONDITIONAL_DIRECTIVE,
        PlSqlTokenType.CONDITIONAL_DECIDED_BOUNDARY,
        PlSqlTokenType.CONDITIONAL_DECIDED_DIRECTIVE -> null
    }

    /**
     * Saves the highlighting of the walked file. Called once the scan returns, under the sensor's lock: a walk without
     * rule checks runs beside the walks of other files.
     */
    fun save() {
        highlighting.save()
    }

    private fun highlight(token: Token, code: TypeOfText) {
        highlighting.highlight(token.line, token.column, token.endLine, token.endColumn, code)
    }

}
