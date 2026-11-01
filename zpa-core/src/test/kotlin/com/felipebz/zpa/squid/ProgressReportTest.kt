//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.squid

import com.felipebz.zpa.utils.log.Logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Duration
import kotlin.concurrent.withLock

class ProgressReportTest {

    @Test
    fun test() {
        assertTimeout(Duration.ofSeconds(5)) {
            val logger: Logger = mock()

            val report = ProgressReport(ProgressReport::class.java.name, 50, logger)

            report.start(listOf("foo.java", "foo.java"))

            // Wait for start message
            waitForMessage(report)

            // Wait for at least one progress message
            waitForMessage(report)

            report.stop()

            // Waits for the thread to die
            // Note: We cannot simply wait for a message here, because it could
            // either be a progress or a stop one
            report.join()

            argumentCaptor<String>().apply {
                verify(logger, atLeast(3)).info(capture())

                assertThat(allValues.size).isGreaterThanOrEqualTo(3)
                assertThat(allValues[0]).isEqualTo("2 source files to be analyzed")
                for (i in 1 until allValues.size - 1) {
                    assertThat(allValues[i]).isEqualTo("0/2 files analyzed, current file: foo.java")
                }
                assertThat(allValues[allValues.size - 1]).isEqualTo("2/2" + " source files have been analyzed")
            }
        }
    }

    @Test
    fun testCancel() {
        assertTimeout(Duration.ofSeconds(5)) {
            val logger: Logger = mock()

            val report = ProgressReport(ProgressReport::class.java.name, 50, logger)
            report.start(listOf("foo.java", "foo.java"))

            // Wait for start message
            waitForMessage(report)

            report.cancel()
            report.join()
        }
    }

    private fun waitForMessage(report: ProgressReport) {
        report.lock.withLock {
            report.condition.await()
        }
    }

}
