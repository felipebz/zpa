package org.sonar.plugins.plsqlopen.api.ddl;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.DdlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class InlineConstraintTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(DdlGrammar.INLINE_CONSTRAINT);
    }
    
    @Test
    public void matchesNotNull() {
        assertThat(p).matches("not null");
    }
    
    @Test
    public void matchesNull() {
        assertThat(p).matches("null");
    }
    
    @Test
    public void matchesUnique() {
        assertThat(p).matches("unique");
    }
    
    @Test
    public void matchesPrimaryKey() {
        assertThat(p).matches("primary key");
    }
    
    @Test
    public void matchesReferences() {
        assertThat(p).matches("references tab (col)");
    }
    
    @Test
    public void matchesReferencesWithSchema() {
        assertThat(p).matches("references sch.tab (col)");
    }
    
    @Test
    public void matchesReferencesWithMoreColumns() {
        assertThat(p).matches("references tab (col, col2, col3)");
    }
    
    @Test
    public void matchesReferencesOnDeleteCascade() {
        assertThat(p).matches("references tab (col) on delete cascade");
    }
    
    @Test
    public void matchesReferencesOnDeleteSetNull() {
        assertThat(p).matches("references tab (col) on delete set null");
    }
    
    @Test
    public void matchesCheck() {
        assertThat(p).matches("check (x > 1)");
    }
    
    @Test
    public void matchesConstraintWithName() {
        assertThat(p).matches("constraint pk primary key");
    }

}
