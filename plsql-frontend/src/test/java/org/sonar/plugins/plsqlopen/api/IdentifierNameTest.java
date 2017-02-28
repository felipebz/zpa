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
package org.sonar.plugins.plsqlopen.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

public class IdentifierNameTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.IDENTIFIER_NAME);
    }

    @Test
    public void matchesSimpleIdentifier() {
        assertThat(p).matches("x");
    }
    
    @Test
    public void matchesIdentifierWithNumber() {
        assertThat(p).matches("t2");
    }
    
    @Test
    public void matchesIdentifierWithNumberSign() {
        assertThat(p).matches("phone#");
        assertThat(p).matches("SN##");
    }
    
    @Test
    public void matchesIdentifierWithUnderscore() {
        assertThat(p).matches("credit_limit");
        assertThat(p).matches("try_again_");
    }
    
    @Test
    public void matchesIdentifierWithDollarSign() {
        assertThat(p).matches("oracle$number");
        assertThat(p).matches("money$$$tree");
    }
    
    @Test
    public void matchesQuotedIdentifier() {
        assertThat(p).matches("\"X+Y\"");
        assertThat(p).matches("\"last name\"");
        assertThat(p).matches("\"on/off switch\"");
        assertThat(p).matches("\"employee(s)\"");
        assertThat(p).matches("\"*** header info ***\"");
    }
    
    @Test
    public void matchesNonReservedKeywords() {
        assertThat(p).matches("cursor");
        assertThat(p).matches("rowid");
    }
    
    @Test
    public void matchesIdentifierWithSpecialCharacters() {
        assertThat(p).matches("variável");
    }
    
    @Test
    public void notMatchesIdentifierStartingWithNumber() {
        assertThat(p).notMatches("2foo");
    }
    
    @Test
    public void notMatchesIdentifierWithAmpersand() {
        assertThat(p).notMatches("mine&yours");
    }
    
    @Test
    public void notMatchesIdentifierWithHyphen() {
        assertThat(p).notMatches("debit-amount");
    }
    
    @Test
    public void notMatchesIdentifierWithSlash() {
        assertThat(p).notMatches("on/off");
    }
    
    @Test
    public void notMatchesIdentifierWithSpace() {
        assertThat(p).notMatches("user id");
    }
    
    @Test
    public void notMatchesQuotedIdentifierCornerCases() {
        assertThat(p).notMatches("\"\"");
        assertThat(p).notMatches("\"\"\"\"");
    }

}
