//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.utplsql

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "testExecutions")
data class TestExecutions @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "version")
    var version: Int = 0,

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "file")
    var files: List<TestFile>? = null
)

@JacksonXmlRootElement(localName = "file")
data class TestFile @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "path")
    var path: String = "",

    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "testCase")
    var testCases: List<TestCase>? = null
)

@JacksonXmlRootElement(localName = "testCase")
data class TestCase @JvmOverloads constructor(
    @field:JacksonXmlProperty(isAttribute = true, localName = "name")
    var name: String = "",

    @field:JacksonXmlProperty(isAttribute = true, localName = "duration")
    var duration: Long = 0,

    @field:JacksonXmlProperty(localName = "skipped")
    var skipped: Skipped? = null,

    @field:JacksonXmlProperty(localName = "failure")
    var failure: Failure? = null,

    @field:JacksonXmlProperty(localName = "error")
    var error: Error? = null
) {
    val status: TestCaseStatus
        get() {
            return when {
                skipped != null -> TestCaseStatus.SKIPPED
                failure != null -> TestCaseStatus.FAILED
                error != null -> TestCaseStatus.ERROR
                else -> TestCaseStatus.PASSED
            }
        }
}

@JacksonXmlRootElement(localName = "skipped")
@JsonIgnoreProperties(ignoreUnknown = true)
class Skipped

@JacksonXmlRootElement(localName = "failure")
@JsonIgnoreProperties(ignoreUnknown = true)
class Failure

@JacksonXmlRootElement(localName = "error")
@JsonIgnoreProperties(ignoreUnknown = true)
class Error

enum class TestCaseStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR
}
