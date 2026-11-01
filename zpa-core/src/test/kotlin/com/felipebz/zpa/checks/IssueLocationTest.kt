//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IssueLocationTest {

    @Test
    fun atFileLevel() {
        val location = IssueLocation.atFileLevel("Issue")
        assertThat(location.startLine()).isEqualTo(0)
        assertThat(location.startLineOffset()).isEqualTo(-1)
        assertThat(location.endLine()).isEqualTo(0)
        assertThat(location.endLineOffset()).isEqualTo(-1)
        assertThat(location.message()).isEqualTo("Issue")
    }

    @Test
    fun atLineLevel() {
        val location = IssueLocation.atLineLevel("Issue", 10)
        assertThat(location.startLine()).isEqualTo(10)
        assertThat(location.startLineOffset()).isEqualTo(-1)
        assertThat(location.endLine()).isEqualTo(10)
        assertThat(location.endLineOffset()).isEqualTo(-1)
        assertThat(location.message()).isEqualTo("Issue")
    }

    @Test
    fun preciseLocationSingleNode() {
        val node = createNode(1, 1, 1, 5)
        val location = IssueLocation.preciseLocation(node, "Issue")
        assertThat(location.startLine()).isEqualTo(1)
        assertThat(location.startLineOffset()).isEqualTo(1)
        assertThat(location.endLine()).isEqualTo(1)
        assertThat(location.endLineOffset()).isEqualTo(4)
        assertThat(location.message()).isEqualTo("Issue")
    }

    @Test
    fun preciseLocationStartAndEndNode() {
        val startNode = createNode(1, 1, 1, 5)
        val endNode = createNode(2, 1, 2, 10)
        val location = IssueLocation.preciseLocation(startNode, endNode, "Issue")
        assertThat(location.startLine()).isEqualTo(1)
        assertThat(location.startLineOffset()).isEqualTo(1)
        assertThat(location.endLine()).isEqualTo(2)
        assertThat(location.endLineOffset()).isEqualTo(9)
        assertThat(location.message()).isEqualTo("Issue")
    }

    private fun createNode(startLine: Int, startCharacter: Int, endLine: Int, endCharacter: Int): AstNode {
        val token = Token.builder()
            .setLine(startLine)
            .setColumn(startCharacter)
            .setValueAndOriginalValue("")
            .setType(GenericTokenType.IDENTIFIER)
            .build()

        val lastToken = Token.builder()
            .setLine(endLine)
            .setColumn(endCharacter - 1)
            .setValueAndOriginalValue("")
            .setType(GenericTokenType.IDENTIFIER)
            .build()

        val node = AstNode(token)
        node.addChild(AstNode(token))
        node.addChild(AstNode(lastToken))

        return node
    }

}
