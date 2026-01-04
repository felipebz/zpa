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
