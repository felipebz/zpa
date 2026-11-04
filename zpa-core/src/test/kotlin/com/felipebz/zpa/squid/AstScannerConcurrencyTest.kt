//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.checks.PlSqlCheck
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Walks of two files on one scanner run at the same time only when the scanner runs no rule check. */
class AstScannerConcurrencyTest {
    private class Source(private val name: String) : PlSqlFile {
        override fun contents() = "begin\n  null;\nend;\n/\n"
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Waits in [visitFile] until both walks have reached it, or [timeoutMillis]; [met] says whether they did. */
    private class Meeting(private val both: CountDownLatch, private val timeoutMillis: Long) : PlSqlCheck() {
        @Volatile var met = false

        override fun visitFile(node: AstNode) {
            both.countDown()
            met = both.await(timeoutMillis, TimeUnit.MILLISECONDS)
        }
    }

    /** Scans two files at once on [scanner], each walk waiting for the other; whether each walk met the other. */
    private fun meetings(scanner: AstScanner, timeoutMillis: Long): List<Boolean> {
        val both = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val walks = listOf("a.sql", "b.sql").map { name ->
                executor.submit<Boolean> {
                    val meeting = Meeting(both, timeoutMillis)
                    scanner.scanFile(Source(name), listOf(meeting))
                    meeting.met
                }
            }
            return walks.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun walksWithoutRuleChecksRunAtTheSameTime() {
        val scanner = AstScanner(emptyList(), null, false)
        assertThat(meetings(scanner, timeoutMillis = 10_000)).containsExactly(true, true)
    }

    @Test
    fun walksWithARuleCheckRunOneAtATime() {
        val scanner = AstScanner(listOf(object : PlSqlCheck() {}), null, false)
        // The first walk gives up waiting while it holds the lock; the second starts only then, and finds it reached.
        assertThat(meetings(scanner, timeoutMillis = 300)).containsExactlyInAnyOrder(false, true)
    }
}
