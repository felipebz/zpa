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
package org.sonar.plugins.plsqlopen.api.statements;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class MergeStatementTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.MERGE_STATEMENT);
    }

    @Test
    public void matchesMergeSimpleMerge() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeValuesWithRecord() {
    	assertThat(p).matches("merge into dest_tab "
    			+ "using source_tab on (1 = 2) "
    			+ "when matched then update set col = val "
    			+ "when not matched then insert values rec;");
    }
    
    @Test
    public void matchesMergeWithInsertFirst() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when not matched then insert values (val)"
                + "when matched then update set col = val ;");
    }
    
    @Test
    public void matchesMergeSimpleMergeWithSourceTableAlias() {
        assertThat(p).matches("merge into dest_tab foo "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeUsingSubquery() {
        assertThat(p).matches("merge into dest_tab "
                + "using (select val from source_tab) on (dest_tab.val = source_tab.val) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeUpdatingMultipleColumns() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col1 = val1, col2 = val2, col3 = val3 "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeInsertingMultipleValues() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (foo, bar, baz);");
    }
    
    @Test
    public void matchesMergeWithoutMatchedClause() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeWithoutNotMatchedClause() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val;");
    }
    
    @Test
    public void matchesMergeWithConditionalInsert() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val) where 3 = 4;");
    }
    
    @Test
    public void matchesMergeWithConditionalUpdate() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val where 3 = 4 "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeWithUpdateDeleteOption() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val delete where 3 = 4 "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeWithConditionalUpdatePlusDeleteOption() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val where 3 = 4 delete where 5 = 6 "
                + "when not matched then insert values (val);");
    }

    @Test
    public void matchesMergeWithInsertColumnsList() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert (foo, bar) values (val);");
    }
    
    @Test
    public void matchesMergeWithInsertColumnsListWithAliases() {
        assertThat(p).matches("merge into dest_tab tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert (tab.foo, tab.bar) values (val);");
    }
    
    @Test
    public void matchesLabeledMerge() {
        assertThat(p).matches("<<foo>>"
                + "merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesForallMerge() {
        assertThat(p).matches("forall x in indices of foo "
                + "merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);");
    }
    
    @Test
    public void matchesMergeWithErrorLoggingClause() {
         assertThat(p).matches("merge into dest_tab d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val)"
                + "log errors into error_tab reject limit 10;");       
    }
      
    @Test
    public void matchesMergeWithDefaultValues() {
         assertThat(p).matches("merge into dest_tab d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col1 = val, col2 = s.val, col3 = default "                 
                + "when not matched then insert values (val, s.val, default);");       
    }   
}
