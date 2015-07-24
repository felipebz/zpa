package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

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

}
