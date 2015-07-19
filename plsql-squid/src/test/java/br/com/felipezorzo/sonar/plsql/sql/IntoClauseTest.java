package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class IntoClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.INTO_CLAUSE);
    }
    
    @Test
    public void matchesSimpleIntoClause() {
        assertThat(p).matches("into var");
    }
    
    @Test
    public void matchesIntoClauseInMultipleVariables() {
        assertThat(p).matches("into var, var2");
    }
    
    @Test
    public void matchesBulkCollect() {
        assertThat(p).matches("bulk collect into var");
    }
    
    @Test
    public void matchesSimpleCollectionElement() {
        assertThat(p).matches("into col(1)");
    }
    
    @Test
    public void matchesSimpleIntoRecordItem() {
        assertThat(p).matches("into col.it");
    }
    
    @Test
    public void matchesSimpleIntoItemInRecordCollection() {
        assertThat(p).matches("into col(1).it");
    }

}
