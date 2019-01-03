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
package org.sonar.plsqlopen.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;
import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.plsqlopen.checks.PlSqlCheck.PreciseIssue;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public class ParsingErrorCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        File file = new File("src/test/resources/checks/parsing_error.sql");

        Parser<Grammar> parser = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
        PlSqlVisitorContext context;
        try {
            parser.parse(file);
            throw new IllegalStateException("Expected RecognitionException");
        } catch (RecognitionException e) {
            context = new PlSqlVisitorContext(null, e, null);
        }

        ParsingErrorCheck check = new ParsingErrorCheck();
        List<PreciseIssue> issues = check.scanFileForIssues(context);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).primaryLocation().startLine()).isEqualTo(1);
    }

}
