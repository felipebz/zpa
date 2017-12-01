/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
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

import org.junit.Test;
import org.sonar.plsqlopen.checks.verifier.PlSqlCheckVerifier;
import org.sonar.plsqlopen.squid.AnalysisException;

public class XPathCheckTest extends BaseCheckTest {
    
    private static final String MESSAGE = "Avoid statements";
    
    @Test
    public void line_level_issue() {
      analyze("xpath_statement.sql", "//STATEMENT");
    }

    @Test
    public void boolean_true_result() {
      analyze("xpath_count_statement.sql", "count(//STATEMENT) > 0");
    }

    @Test
    public void boolean_false_result() {
      analyze("xpath.sql", "count(//STATEMENT) < 0");
    }

    @Test
    public void integer_xpath_result() throws Exception {
      analyze("xpath.sql", "count(//STATEMENT)");
    }

    @Test
    public void empty_query() throws Exception {
      analyze("xpath.sql", "");
    }

    @Test(expected = AnalysisException.class)
    public void invalid_query() throws Exception {
      analyze("xpath.sql", "+++");
  }
    
    private void analyze(String fileName, String xpathQuery) {
        XPathCheck check = new XPathCheck();
        check.xpathQuery = xpathQuery;
        check.message = MESSAGE;
        PlSqlCheckVerifier.verify("src/test/resources/checks/" + fileName, check);
    }

}
