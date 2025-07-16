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
package com.felipebz.zpa.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token

abstract class IssueLocation private constructor(private val message: String) {

    fun message() = message

    abstract fun startLine(): Int

    abstract fun startLineOffset(): Int

    abstract fun endLine(): Int

    abstract fun endLineOffset(): Int

    private class PreciseIssueLocation : IssueLocation {

        private val firstToken: Token
        private val lastToken: Token

        constructor(node: AstNode, message: String) : super(message) {
            this.firstToken = node.token
            this.lastToken = node.lastToken
        }

        constructor(startNode: AstNode, endNode: AstNode, message: String) : super(message) {
            this.firstToken = startNode.token
            this.lastToken = endNode.lastToken
        }

        override fun startLine() = firstToken.line

        override fun startLineOffset() = firstToken.column

        override fun endLine() = lastToken.endLine

        override fun endLineOffset() = lastToken.endColumn

    }

    private class LineLevelIssueLocation(message: String, private val lineNumber: Int) : IssueLocation(message) {

        override fun startLine() = lineNumber

        override fun startLineOffset() = UNDEFINED_OFFSET

        override fun endLine() = lineNumber

        override fun endLineOffset() = UNDEFINED_OFFSET

    }

    private class FileLevelIssueLocation(message: String) : IssueLocation(message) {

        override fun startLine() = UNDEFINED_LINE

        override fun startLineOffset() = UNDEFINED_OFFSET

        override fun endLine() = UNDEFINED_LINE

        override fun endLineOffset() = UNDEFINED_OFFSET

    }

    companion object {

        const val UNDEFINED_OFFSET = -1

        const val UNDEFINED_LINE = 0

        fun atFileLevel(message: String): IssueLocation =
            FileLevelIssueLocation(message)

        fun atLineLevel(message: String, lineNumber: Int): IssueLocation =
            LineLevelIssueLocation(message, lineNumber)

        fun preciseLocation(startNode: AstNode, endNode: AstNode, message: String): IssueLocation =
            PreciseIssueLocation(startNode, endNode, message)

        fun preciseLocation(startNode: AstNode, message: String): IssueLocation =
            PreciseIssueLocation(startNode, message)
    }
}
