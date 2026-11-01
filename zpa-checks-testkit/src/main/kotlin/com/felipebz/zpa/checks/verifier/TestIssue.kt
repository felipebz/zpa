//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.checks.verifier

internal class TestIssue constructor(var message: String?, val line: Int) {
    var effortToFix: Int? = null
    var startColumn: Int? = null
    var endColumn: Int? = null
    var endLine: Int? = null
    var secondaryLines: List<Int>? = null

    override fun toString() = "$message[line=$line]"
}
