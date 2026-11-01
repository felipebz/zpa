//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.flr.internal.toolkit

import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.PlSqlPunctuator
import com.felipebz.zpa.api.PlSqlTokenType
import java.awt.Color
import javax.swing.text.*


internal object SourceCodeStyler {
    val defaultStyle: Style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE)
    private val keywordStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, Color.BLUE)
        StyleConstants.setBold(this, true)
    }
    private val literalStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, Color(0x800080))
    }
    private val commentStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, Color(0x078C39))
    }
    private val punctuatorStyle = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, Color.RED)
    }

    fun style(styledDocument: StyledDocument, lineOffsets: LineOffsets, tokens: List<Token>) {
        styledDocument.setCharacterAttributes(0, styledDocument.length, defaultStyle, true)

        for (token in tokens) {
            for (trivia in token.trivia) {
                styledDocument.setCharacterAttributes(
                    lineOffsets.getStartOffset(trivia.token),
                    trivia.token.originalValue.length,
                    commentStyle,
                    false
                )
            }
            val style = when (token.type) {
                is PlSqlKeyword -> keywordStyle
                is PlSqlTokenType -> literalStyle
                is PlSqlPunctuator -> if (token.type != PlSqlPunctuator.SEMICOLON) punctuatorStyle else null
                else -> null
            }
            if (style != null) {
                styledDocument.setCharacterAttributes(
                    lineOffsets.getStartOffset(token),
                    token.originalValue.length,
                    style,
                    false
                )
            }
        }
    }
}
