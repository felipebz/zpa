package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

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
