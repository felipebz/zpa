/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.utplsql

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "coverage")
data class Coverage @JvmOverloads constructor(
    @field:Attribute(name = "version")
    var version: Int = 0,

    @field:ElementList(name = "file", inline = true, required = false)
    var files: List<CoveredFile>? = null
)

@Root(name = "file")
data class CoveredFile @JvmOverloads constructor(
    @field:Attribute(name = "path")
    var path: String = "",

    @field:ElementList(name = "lineToCover", inline = true, required = false)
    var linesToCover: List<LineToCover>? = null
)

@Root(name = "lineToCover")
data class LineToCover @JvmOverloads constructor(
    @field:Attribute(name = "lineNumber")
    var lineNumber: Int = 0,

    @field:Attribute(name = "covered")
    var covered: Boolean = false,

    @field:Attribute(name = "branchesToCover", required = false)
    var branchesToCover: Int? = null,

    @field:Attribute(name = "coveredBranches", required = false)
    var coveredBranches: Int? = null
)
