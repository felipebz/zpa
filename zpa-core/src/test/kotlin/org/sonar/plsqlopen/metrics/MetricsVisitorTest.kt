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
package org.sonar.plsqlopen.metrics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.plsqlopen.TestPlSqlVisitorRunner
import java.io.File

class MetricsVisitorTest {

    private val visitor = MetricsVisitor()

    @Test
    fun statements() {
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/metrics/statements.sql"), null, visitor)
        assertThat(visitor.numberOfStatements).isEqualTo(4)
    }

    @Test
    fun lines_of_code() {
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/metrics/lines_of_code.sql"), null, visitor)
        assertThat(visitor.getLinesOfCode()).containsOnly(1, 4, 5, 6)
    }

    @Test
    fun comments() {
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/metrics/comments.sql"), null, visitor)
        assertThat(visitor.getLinesOfComments()).containsOnly(2, 4)
    }

    @Test
    fun no_sonar() {
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/metrics/no_sonar.sql"), null, visitor)
        assertThat(visitor.linesWithNoSonar).containsOnly(3, 4)
    }

    @Test
    fun executable_lines() {
        TestPlSqlVisitorRunner.scanFile(File("src/test/resources/metrics/statements.sql"), null, visitor)
        assertThat(visitor.getExecutableLines()).containsOnly(2, 3, 4, 5)
    }

}
