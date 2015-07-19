package br.com.felipezorzo.sonar.plsql.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class AssignmentStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ASSIGNMENT_STATEMENT);
    }
    
    @Test
    public void assignmentToVariable() {
        assertThat(p).matches("var := 1;");
    }
    
    @Test
    public void assignmentToRecordAttribute() {
        assertThat(p).matches("rec.attribute := 1;");
    }
    
    @Test
    public void assignmentToCollectionElement() {
        assertThat(p).matches("collection(1) := 1;");
    }
    
    @Test
    public void assignmentToItemInRecordCollection() {
        assertThat(p).matches("collection(1).field := 1;");
    }
    
    @Test
    public void assignmentToHostVariable() {
        assertThat(p).matches(":var := 1;");
    }
    
    @Test
    public void assignmentToIndicatorVariable() {
        assertThat(p).matches(":var:indicator := 1;");
    }

}
