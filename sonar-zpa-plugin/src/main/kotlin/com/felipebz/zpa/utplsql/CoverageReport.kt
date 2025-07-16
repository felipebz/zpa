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
package com.felipebz.zpa.utplsql

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "coverage")
data class Coverage @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "version")
    var version: Int = 0,

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "file")
    var files: List<CoveredFile>? = null
)

@JacksonXmlRootElement(localName = "file")
data class CoveredFile @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "path")
    var path: String = "",

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "lineToCover")
    var linesToCover: List<LineToCover>? = null
)

@JacksonXmlRootElement(localName = "lineToCover")
data class LineToCover @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "lineNumber")
    var lineNumber: Int = 0,

    @field:JacksonXmlProperty(isAttribute = true, localName = "covered")
    var covered: Boolean = false,

    @field:JacksonXmlProperty(isAttribute = true, localName = "branchesToCover")
    var branchesToCover: Int? = null,

    @field:JacksonXmlProperty(isAttribute = true, localName = "coveredBranches")
    var coveredBranches: Int? = null
)
