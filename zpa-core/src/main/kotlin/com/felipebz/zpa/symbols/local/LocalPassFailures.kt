//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.utils.log.Logger

/**
 * What a failure of the local pass for one file does. An analysis logs it and goes on: the file's local facts stay
 * absent and its readers answer that the file was not analyzed. zpa's own test runs set the system property
 * [PROPERTY] (the Gradle test tasks of the build pass it), and then the failure fails the scan, naming the file and the
 * exception, so that the tests and the ruling cannot pass over a file the local pass did not analyze.
 */
internal object LocalPassFailures {
    /** Set by zpa's build for its own test tasks; not an option of the analyzer. */
    const val PROPERTY = "com.felipebz.zpa.internal.failOnLocalPassError"

    /** Logs [error] with [message] and returns null; under zpa's own tests, throws it instead. */
    fun report(log: Logger, message: String, error: Throwable): Nothing? {
        if (java.lang.Boolean.getBoolean(PROPERTY)) throw IllegalStateException("$message: $error", error)
        log.warn("$message: $error")
        return null
    }
}
