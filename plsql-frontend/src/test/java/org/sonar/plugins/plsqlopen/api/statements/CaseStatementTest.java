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
package org.sonar.plugins.plsqlopen.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CaseStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CASE_STATEMENT);
    }

    @Test
    public void matchesSimpleSearchedCase() {
        assertThat(p).matches("case when x = 1 then foo := bar; end case;");
    }
    
    @Test
    public void matchesSimpleCase() {
        assertThat(p).matches("case x when 1 then foo := bar; end case;");
    }
    
    @Test
    public void matchesCaseWithMultipleWhen() {
        assertThat(p).matches("case x when 1 then foo := bar; when 2 then foo := bar; end case;");
    }
    
    @Test
    public void matchesCaseWithElse() {
        assertThat(p).matches("case x when 1 then foo := bar; else foo := bar; end case;");
    }
    
    @Test
    public void matchesCaseWithMultipleStataments() {
        assertThat(p).matches("case when x = 1 then foo := bar; bar := baz; end case;");
    }
    
    @Test
    public void matchesCaseWithMemberIdentifier() {
        assertThat(p).matches("case foo.bar when 1 then foo := bar; end case;");
    }
    
    @Test
    public void matchesLabeledCase() {
        assertThat(p).matches("<<foo>> case when x = 1 then foo := bar; end case foo;");
    }
    
    @Test
    public void matchesCaseWithSelectorExpression() {
        assertThat(p).matches("case foo + bar when 1 then foo := bar; end case;");
    }
    
    @Test
    public void matchesBooleanExpressionSearchedCase() {
        assertThat(p).matches("case when foo is not null and bar is null then foo := bar; end case;");
    }
    

}
