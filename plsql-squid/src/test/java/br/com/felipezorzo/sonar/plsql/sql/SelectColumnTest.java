package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class SelectColumnTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SELECT_COLUMN);
    }
    
    @Test
    public void matchesAllColumns() {
        assertThat(p).matches("*");
    }
    
    @Test
    public void matchesFunctionCall() {
        assertThat(p).matches("func(var)");
    }
    
    @Test
    public void matchesColumnName() {
        assertThat(p).matches("name");
    }
    
    @Test
    public void matchesTableColumn() {
        assertThat(p).matches("tab.name");
    }
    
    @Test
    public void matchesAllTableColumns() {
        assertThat(p).matches("tab.*");
    }
    
    @Test
    public void matchesTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.name");
    }
    
    @Test
    public void matchesAllTableColumnInDiffentSchema() {
        assertThat(p).matches("sch.tab.*");
    }
    
    @Test
    public void matchesLiterals() {
        assertThat(p).matches("'x'");
        assertThat(p).matches("1");
        assertThat(p).matches("null");
    }
    
    @Test
    public void matchesSequenceCurrentValue() {
        assertThat(p).matches("seq.currval");
    }
    
    @Test
    public void matchesSequenceNextValue() {
        assertThat(p).matches("seq.nextval");
    }
    
    @Test
    public void matchesColumnWithAlias() {
        assertThat(p).matches("null alias");
    }
    
    @Test
    public void matchesColumnWithAliasWithAsKeyword() {
        assertThat(p).matches("null as alias");
    }

}
