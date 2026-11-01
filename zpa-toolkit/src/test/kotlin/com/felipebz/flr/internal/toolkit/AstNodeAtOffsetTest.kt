//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
