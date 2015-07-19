package br.com.felipezorzo.sonar.plsql.api;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class RefDatatypeTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.REF_DATATYPE);
    }
    
    @Test
    public void matchesSimpleRef() {
        assertThat(p).matches("ref custom");
    }
    
    @Test
    public void matchesRefCursor() {
        assertThat(p).matches("ref cursor");
    }

}
