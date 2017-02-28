/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class SimpleDmlTriggerTest extends RuleTest {

    private final String body = " begin null; end;";
    
    @Before
    public void init() {
        setRootRule(PlSqlGrammar.SIMPLE_DML_TRIGGER);
    }
    
    @Test
    public void matchesBefore() {
        assertThat(p).matches("before insert on tab" + body);
    }
    
    @Test
    public void matchesAfter() {
        assertThat(p).matches("after insert on tab" + body);
    }
    
    @Test
    public void matchesUpdate() {
        assertThat(p).matches("before update on tab" + body);
    }
    
    @Test
    public void matchesUpdateWithOneColumn() {
        assertThat(p).matches("before update of col1 on tab" + body);
    }
    
    @Test
    public void matchesInsertOfWithOneColumn() {
        assertThat(p).matches("before insert of col1 on tab" + body);
    }
    
    @Test
    public void matchesDeleteOfWithOneColumn() {
        assertThat(p).matches("before delete of col1 on tab" + body);
    }
    
    @Test
    public void matchesUpdateWithMultipleColumns() {
        assertThat(p).matches("before update of col1, col2 on tab" + body);
    }
    
    @Test
    public void matchesDelete() {
        assertThat(p).matches("before delete on tab" + body);
    }
    
    @Test
    public void matchesReferencing() {
        assertThat(p).matches("after insert on tab referencing old as foo" + body);
    }
    
    @Test
    public void matchesMultipleReferencingClause() {
        assertThat(p).matches("after insert on tab referencing old foo new as bar parent as bar" + body);
    }
    
    @Test
    public void matchesForEachRow() {
        assertThat(p).matches("after insert on tab for each row" + body);
    }
    
    @Test
    public void matchesTriggerEditionForward() {
        assertThat(p).matches("after insert on tab forward crossedition" + body);
    }
    
    @Test
    public void matchesTriggerEditionReverse() {
        assertThat(p).matches("after insert on tab reverse crossedition" + body);
    }
    
    @Test
    public void matchesTriggerOrderingFollows() {
        assertThat(p).matches("after insert on tab follows foo" + body);
    }
    
    @Test
    public void matchesTriggerOrderingPrecedes() {
        assertThat(p).matches("after insert on tab precedes foo" + body);
    }
    
    @Test
    public void matchesTriggerOrderingFollowsMultiple() {
        assertThat(p).matches("after insert on tab follows foo, bar" + body);
    }

}
