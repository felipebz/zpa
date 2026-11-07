//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.squid

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.semantics.HighlightingPair
import com.felipebz.zpa.api.symbols.Symbol

data class AstScannerResult internal constructor(
    val executedChecks: List<PlSqlVisitor>,
    val linesWithNoSonar: Set<Int>,
    /**
     * Deprecated: part of the legacy symbol model, removed in the next major version. Use highlightingPairs for
     * declarations and their references, or PlSqlVisitorContext.nameBinding for one name.
     */
    @Deprecated(
        "Part of the legacy symbol model. It will be removed in the next major version. Use highlightingPairs for " +
        "declarations and their references, or PlSqlVisitorContext.nameBinding for one name."
    )
    @Suppress("DEPRECATION")
    val symbols: List<Symbol>,
    val numberOfStatements: Int,
    val linesOfCode: Int,
    val linesOfComments: Int,
    val complexity: Int,
    val numberOfFunctions:  Int,
    val executableLines: Set<Int>,
    val issues: List<ZpaIssue>,
    /**
     * The file's highlighting pairs from its local name resolution; empty when the scanner was not asked for them, or
     * when the file was not analyzed (a parse failure, or a local resolution that failed).
     */
    @property:ZpaExperimentalApi
    val highlightingPairs: List<HighlightingPair> = emptyList()
)

data class ZpaIssue internal constructor(
    val file: PlSqlFile,
    val check: PlSqlCheck,
    private val issue: PlSqlCheck.PreciseIssue
) {
    val cost = issue.cost()
    val primaryLocation = issue.primaryLocation()
    val secondaryLocations = issue.secondaryLocations()
}
