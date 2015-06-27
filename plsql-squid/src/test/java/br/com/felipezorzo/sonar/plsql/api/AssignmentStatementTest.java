package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class AssignmentStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.ASSIGNMENT_STATEMENT);
    }
    
    @Test
    public void assignmentToVariable() {
        assertThat(p).matches("var := 1");
    }
    
    @Test
    public void assignmentToRecordAttribute() {
        assertThat(p).matches("record.attribute := 1");
    }
    
    @Test
    public void assignmentToCollectionElement() {
        assertThat(p).matches("collection(1) := 1");
    }
    
    @Test
    public void assignmentToHostVariable() {
        assertThat(p).matches(":var := 1");
    }
    
    @Test
    public void assignmentToIndicatorVariable() {
        assertThat(p).matches(":var:indicator := 1");
    }

}
