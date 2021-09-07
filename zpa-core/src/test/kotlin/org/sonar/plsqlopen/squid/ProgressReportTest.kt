/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.sonar.plsqlopen.utils.log.Logger
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
