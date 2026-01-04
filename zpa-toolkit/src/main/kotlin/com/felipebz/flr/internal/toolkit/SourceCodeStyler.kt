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
