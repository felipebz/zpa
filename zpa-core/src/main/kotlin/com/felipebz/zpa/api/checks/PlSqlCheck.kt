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
package com.felipebz.zpa.api.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.checks.IssueLocation
import com.felipebz.zpa.sslr.Tree
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.squid.SemanticAstNode
import java.text.MessageFormat
import java.util.*

open class PlSqlCheck : PlSqlVisitor() {

    private val issues = ArrayList<PreciseIssue>()

    override fun startScan() {
        issues.clear()
    }

    fun semantic(node: AstNode) = node as SemanticAstNode

    fun issues(): List<PreciseIssue> = Collections.unmodifiableList(issues)

    fun scanFileForIssues(context: PlSqlVisitorContext): List<PreciseIssue> {
        issues.clear()
        scanFile(context)
        return issues()
    }

    fun addIssue(node: AstNode, message: String): PreciseIssue {
        val newIssue = PreciseIssue(IssueLocation.preciseLocation(node, message))
        issues.add(newIssue)
        return newIssue
    }

    fun addIssue(node: AstNode, message: String, vararg messageParameters: Any): PreciseIssue {
        return addIssue(node, MessageFormat.format(message, *messageParameters))
    }

    fun addIssue(tree: Tree, message: String): PreciseIssue {
        val newIssue = PreciseIssue(IssueLocation.preciseLocation(tree.astNode, message))
        issues.add(newIssue)
        return newIssue
    }

    fun addIssue(tree: Tree, message: String, vararg messageParameters: Any): PreciseIssue {
        return addIssue(tree, MessageFormat.format(message, *messageParameters))
    }

    fun addIssue(primaryLocation: IssueLocation): PreciseIssue {
        val newIssue = PreciseIssue(primaryLocation)
        issues.add(newIssue)
        return newIssue
    }

    fun addLineIssue(message: String, lineNumber: Int): PreciseIssue {
        val newIssue = PreciseIssue(IssueLocation.atLineLevel(message, lineNumber))
        issues.add(newIssue)
        return newIssue
    }

    fun addLineIssue(message: String, lineNumber: Int, vararg messageParameters: Any): PreciseIssue {
        return addLineIssue(MessageFormat.format(message, *messageParameters), lineNumber)
    }

    fun addFileIssue(message: String): PreciseIssue {
        val newIssue = PreciseIssue(IssueLocation.atFileLevel(message))
        issues.add(newIssue)
        return newIssue
    }

    fun addFileIssue(message: String, vararg messageParameters: Any): PreciseIssue {
        return addFileIssue(MessageFormat.format(message, *messageParameters))
    }

    fun addIssue(token: Token, message: String): PreciseIssue {
        return addIssue(AstNode(token), message)
    }

    fun addIssue(token: Token, message: String, vararg messageParameters: Any): PreciseIssue {
        return addIssue(token, MessageFormat.format(message, *messageParameters))
    }

    class PreciseIssue(private val primaryLocation: IssueLocation) {
        private var cost: Int? = null
        private val secondaryLocations = mutableListOf<IssueLocation>()

        fun cost() = cost

        fun withCost(cost: Int) = apply {
            this.cost = cost
        }

        fun primaryLocation() = primaryLocation

        fun secondary(node: AstNode, message: String) = apply {
            secondaryLocations.add(IssueLocation.preciseLocation(node, message))
        }

        fun secondary(issueLocation: IssueLocation)= apply {
            secondaryLocations.add(issueLocation)
        }

        fun secondaryLocations(): List<IssueLocation> = secondaryLocations
    }
}
