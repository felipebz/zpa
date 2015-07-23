package br.com.felipezorzo.sonar.plsql.sql;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.RuleTest;

public class HierarchicalQueryClauseTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.HIERARCHICAL_QUERY_CLAUSE);
    }
    
    @Test
    public void matchesSimpleHierarchical() {
        assertThat(p).matches("connect by foo = bar");
    }
    
    @Test
    public void matchesHierarchicalQueryConnectByFirst() {
        assertThat(p).matches("connect by foo = bar start with foo = bar");
    }
    
    @Test
    public void matchesHierarchicalQueryStartWithFirst() {
        assertThat(p).matches("start with foo = bar connect by foo = bar");
    }
    
    @Test
    public void notMatchesStartWithFirstWithoutConnectBy() {
        assertThat(p).notMatches("start with foo = bar");
    }

}
