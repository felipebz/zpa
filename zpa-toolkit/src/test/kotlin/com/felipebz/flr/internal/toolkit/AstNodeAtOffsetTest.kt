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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.toolkit.ZpaConfigurationModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AstNodeAtOffsetTest {
    private val source = "begin\n  null;\nend;"
    private val root = SourceCodeModel(ZpaConfigurationModel()).apply { setSourceCode(source) }.astNode
    private val lineOffsets = LineOffsets(source)

    @Test
    fun caretInsideTokenSelectsThatToken() {
        assertThat(tokenAt(9)).isEqualTo("null")
    }

    @Test
    fun caretAtTokenStartSelectsThatToken() {
        assertThat(tokenAt(8)).isEqualTo("null")
    }

    @Test
    fun caretOutsideAnyTokenSelectsTheFollowingToken() {
        assertThat(tokenAt(13)).isEqualTo("end")
    }

    private fun tokenAt(offset: Int): String? =
        astNodeAtOffset(breadthFirst(root), offset, lineOffsets)?.token?.originalValue

    private fun breadthFirst(root: AstNode): Sequence<AstNode> = sequence {
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            yield(node)
            queue.addAll(node.children)
        }
    }
}
