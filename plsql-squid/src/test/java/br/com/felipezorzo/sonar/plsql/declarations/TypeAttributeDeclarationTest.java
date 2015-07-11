package br.com.felipezorzo.sonar.plsql.declarations;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class TypeAttributeDeclarationTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.TYPE_ATTRIBUTE_DECLARATION);
    }

    @Test
    public void matchesSimpleTypeAttribute() {
        assertThat(p).matches("name%type");
    }
    
    @Test
    public void matchesTableColumn() {
        assertThat(p).matches("table.name%type");
    }
    
    @Test
    public void matchesTableColumnWithExplicitSchema() {
        assertThat(p).matches("schema.table.name%type");
    }

}
