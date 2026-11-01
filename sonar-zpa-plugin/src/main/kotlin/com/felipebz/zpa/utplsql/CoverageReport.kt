//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
