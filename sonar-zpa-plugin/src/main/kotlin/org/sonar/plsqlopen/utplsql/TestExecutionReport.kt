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
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "testExecutions")
data class TestExecutions @JvmOverloads constructor(
    @field:Attribute(name = "version")
    var version: Int = 0,

    @field:ElementList(name = "file", inline = true, required = false)
    var files: List<TestFile>? = null
)

@Root(name = "file")
data class TestFile @JvmOverloads constructor(
    @field:Attribute(name = "path")
    var path: String = "",

    @field:ElementList(name = "testCase", inline = true, required = false)
    var testCases: List<TestCase>? = null
)

@Root(name = "testCase")
data class TestCase @JvmOverloads constructor(
    @field:Attribute(name = "name")
    var name: String = "",

    @field:Attribute(name = "duration")
    var duration: Long = 0,

    @field:Element(name = "skipped", required = false)
    var skipped: Skipped? = null,

    @field:Element(name = "failure", required = false)
    var failure: Failure? = null,

    @field:Element(name = "error", required = false)
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

@Root(name = "skipped", strict = false)
class Skipped

@Root(name = "failure", strict = false)
class Failure

@Root(name = "error", strict = false)
class Error

enum class TestCaseStatus {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR
}
