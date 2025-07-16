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
package com.felipebz.zpa.squid

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.symbols.Symbol

data class AstScannerResult internal constructor(
    val executedChecks: List<PlSqlVisitor>,
    val linesWithNoSonar: Set<Int>,
    val symbols: List<Symbol>,
    val numberOfStatements: Int,
    val linesOfCode: Int,
    val linesOfComments: Int,
    val complexity: Int,
    val numberOfFunctions:  Int,
    val executableLines: Set<Int>,
    val issues: List<ZpaIssue>
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
