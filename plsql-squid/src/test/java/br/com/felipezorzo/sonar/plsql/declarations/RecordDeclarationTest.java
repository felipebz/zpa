package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class RecordDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.RECORD_DECLARATION);
    }

    @Test
    public void matchesSimpleRecord() {
        assertThat(p).matches("type foo is record(x number);");
    }
    
    @Test
    public void matchesRecordWithMultipleFields() {
        assertThat(p).matches("type foo is record(x number, y number);");
    }

}
