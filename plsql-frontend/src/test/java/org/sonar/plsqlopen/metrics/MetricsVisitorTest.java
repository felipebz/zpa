/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plsqlopen.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Test;
import org.sonar.plsqlopen.TestPlSqlVisitorRunner;

public class MetricsVisitorTest {

    private MetricsVisitor visitor = new MetricsVisitor();

    @Test
    public void statements() {
        TestPlSqlVisitorRunner.scanFile(new File("src/test/resources/metrics/statements.sql"), null, visitor);
        assertThat(visitor.getNumberOfStatements()).isEqualTo(4);
    }
    
    @Test
    public void lines_of_code() {
        TestPlSqlVisitorRunner.scanFile(new File("src/test/resources/metrics/lines_of_code.sql"), null, visitor);
        assertThat(visitor.getLinesOfCode()).containsOnly(1, 4, 5, 6);
    }

    @Test
    public void comments() {
        TestPlSqlVisitorRunner.scanFile(new File("src/test/resources/metrics/comments.sql"), null, visitor);
        assertThat(visitor.getLinesOfComments()).containsOnly(2, 4);
    }
    
    @Test
    public void no_sonar() {
        TestPlSqlVisitorRunner.scanFile(new File("src/test/resources/metrics/no_sonar.sql"), null, visitor);
        assertThat(visitor.getLinesWithNoSonar()).containsOnly(3, 4);
    }
    
    @Test
    public void executable_lines() {
        TestPlSqlVisitorRunner.scanFile(new File("src/test/resources/metrics/statements.sql"), null, visitor);
        assertThat(visitor.getExecutableLines()).containsOnly(2, 3, 4, 5);
    }

}
