package org.sonar.plugins.plsqlopen.api.ddl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class OutOfLineConstraintTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.OUT_OF_LINE_CONSTRAINT);
    }
    
    @Test
    public void matchesUnique() {
        assertThat(p).matches("unique (foo)");
    }
    
    @Test
    public void matchesUniqueWithMoreColumns() {
        assertThat(p).matches("unique (foo, bar, baz)");
    }
    
    @Test
    public void matchesPrimaryKey() {
        assertThat(p).matches("primary key (foo)");
    }
    
    @Test
    public void matchesPrimaryKeyWithMoreColumns() {
        assertThat(p).matches("primary key (foo, bar, baz)");
    }
    
    @Test
    public void matchesForeignKey() {
        assertThat(p).matches("foreign key (col) references tab (col)");
    }
    
    @Test
    public void matchesReferencesWithMoreColumns() {
        assertThat(p).matches("foreign key (col, col2, col3) references tab (col, col2, col3)");
    }
    
    @Test
    public void matchesCheck() {
        assertThat(p).matches("check (x > 1)");
    }
    
    @Test
    public void matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key (foo)");
    }

}
