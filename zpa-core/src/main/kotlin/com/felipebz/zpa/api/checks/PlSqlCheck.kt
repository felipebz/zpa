//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.checks

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectAnalysis
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.api.syntax.SyntaxView
import com.felipebz.zpa.checks.IssueLocation
import com.felipebz.zpa.squid.createNotPreparedProjectAnalysis
import java.text.MessageFormat
import java.util.*

@OptIn(ZpaExperimentalApi::class)
open class PlSqlCheck : PlSqlVisitor() {

    private val issues = ArrayList<PreciseIssue>()
    private var projectAnalysisForScan: ProjectAnalysis = createNotPreparedProjectAnalysis()

    override fun startScan() {
        issues.clear()
    }

    fun semantic(node: AstNode) = node as SemanticAstNode

    /** Returns the read-only project capability for the current analysis run. */
    @ZpaExperimentalApi
    protected fun projectAnalysis(): ProjectAnalysis = projectAnalysisForScan

    @JvmSynthetic
    internal fun installProjectAnalysisForScan(projectAnalysis: ProjectAnalysis) {
        projectAnalysisForScan = projectAnalysis
    }

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

    @ZpaExperimentalApi
    fun addIssue(view: SyntaxView, message: String): PreciseIssue {
        return addIssue(view.astNode, message)
    }

    @ZpaExperimentalApi
    fun addIssue(view: SyntaxView, message: String, vararg messageParameters: Any): PreciseIssue {
        return addIssue(view, MessageFormat.format(message, *messageParameters))
    }

    fun addIssue(node: AstNode, message: String, vararg messageParameters: Any): PreciseIssue {
        return addIssue(node, MessageFormat.format(message, *messageParameters))
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
