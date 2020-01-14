/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2020 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.squid

import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor
import org.sonar.plugins.plsqlopen.api.symbols.Symbol

data class AstScannerResult internal constructor(
    val executedChecks: List<PlSqlVisitor>,
    val linesWithNoSonar: Set<Int>,
    val symbols: List<Symbol>,
    val numberOfStatements: Int,
    val linesOfCode: Int,
    val linesOfComments: Int,
    val complexity: Int,
    val numberOfFunctions:  Int,
    val executableLines: Set<Int>
)
