//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
