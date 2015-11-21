/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plsqlopen.squid;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;

import org.junit.Test;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plugins.plsqlopen.api.PlSqlMetric;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.Grammar;

public class PlSqlAstScannerTest {
    
    private Collection<SquidAstVisitor<Grammar>> noVisitors = null;
    private SonarComponents components = null;
    
    @Test
    public void files() {
        AstScanner<Grammar> scanner = PlSqlAstScanner.create(new PlSqlConfiguration(Charsets.UTF_8), components, noVisitors);
        scanner.scanFiles(ImmutableList.of(new File("src/test/resources/metrics/lines.sql"), new File("src/test/resources/metrics/comments.sql")));
        SourceProject project = (SourceProject) scanner.getIndex().search(new QueryByType(SourceProject.class)).iterator().next();
        assertThat(project.getInt(PlSqlMetric.FILES)).isEqualTo(2);
    }
    
    @Test
    public void lines() {
      SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/metrics/lines.sql"), components, noVisitors);
      assertThat(file.getInt(PlSqlMetric.LINES)).isEqualTo(5);
    }
    
    @Test
    public void comments() {
      SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/metrics/comments.sql"), components, noVisitors);
      assertThat(file.getInt(PlSqlMetric.COMMENT_LINES)).isEqualTo(2);
    }
    
    @Test
    public void lines_of_code() {
      SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/metrics/lines_of_code.sql"), components, noVisitors);
      assertThat(file.getInt(PlSqlMetric.LINES_OF_CODE)).isEqualTo(4);
    }
    
    @Test
    public void methods() {
      SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/metrics/methods.sql"), components, noVisitors);
      assertThat(file.getInt(PlSqlMetric.METHODS)).isEqualTo(4);
    }
    
    @Test
    public void statements() {
      SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/metrics/statements.sql"), components, noVisitors);
      assertThat(file.getInt(PlSqlMetric.STATEMENTS)).isEqualTo(4);
    }
    
}
