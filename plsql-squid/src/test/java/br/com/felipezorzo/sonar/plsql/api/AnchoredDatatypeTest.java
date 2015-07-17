package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

public class AnchoredDatatypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ANCHORED_DATATYPE);
    }

    @Test
    public void matchesSimpleTypeAttribute() {
        assertThat(p).matches("name%type");
    }
    
    @Test
    public void matchesTableColumn() {
        assertThat(p).matches("tab.name%type");
    }
    
    @Test
    public void matchesTableColumnWithExplicitSchema() {
        assertThat(p).matches("schema.tab.name%type");
    }

}
