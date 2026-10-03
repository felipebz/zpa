/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.api.statements

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.DmlGrammar
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class MergeStatementTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.MERGE_STATEMENT)
    }

    @Test
    fun matchesMergeSimpleMerge() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeValuesWithRecord() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values rec;")
    }

    @Test
    fun matchesMergeWithInsertFirst() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when not matched then insert values (val)"
                + "when matched then update set col = val ;")
    }

    @Test
    fun matchesMergeSimpleMergeWithSourceTableAlias() {
        assertThat(p).matches("merge into dest_tab foo "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeUsingSubquery() {
        assertThat(p).matches("merge into dest_tab "
                + "using (select val from source_tab) on (dest_tab.val = source_tab.val) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeUpdatingMultipleColumns() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col1 = val1, col2 = val2, col3 = val3 "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeInsertingMultipleValues() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (foo, bar, baz);")
    }

    @Test
    fun matchesMergeWithoutMatchedClause() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeWithoutNotMatchedClause() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val;")
    }

    @Test
    fun matchesMergeWithConditionalInsert() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val) where 3 = 4;")
    }

    @Test
    fun matchesMergeWithConditionalUpdate() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val where 3 = 4 "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeWithUpdateDeleteOption() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val delete where 3 = 4 "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeWithConditionalUpdatePlusDeleteOption() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val where 3 = 4 delete where 5 = 6 "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeWithInsertColumnsList() {
        assertThat(p).matches("merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert (foo, bar) values (val);")
    }

    @Test
    fun matchesMergeWithInsertColumnsListWithAliases() {
        assertThat(p).matches("merge into dest_tab tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert (tab.foo, tab.bar) values (val);")
    }

    @Test
    fun matchesLabeledMerge() {
        assertThat(p).matches("<<foo>>"
                + "merge into dest_tab "
                + "using source_tab on (1 = 2) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val);")
    }

    @Test
    fun matchesMergeWithErrorLoggingClause() {
        assertThat(p).matches("merge into dest_tab d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col = val "
                + "when not matched then insert values (val)"
                + "log errors into error_tab reject limit 10;")
    }

    @Test
    fun matchesMergeIntoSubquery() {
        assertThat(p).matches("merge into (select a from foo where b = 1) d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col1 = val;")
    }

    @Test
    fun matchesMergeIntoInlineViewWithoutPartitionExtension() {
        assertThat(p).matches("merge into (select id, val from dest_tab) d "
                + "using source_tab s "
                + "on (d.id = s.id) "
                + "when matched then update set d.val = s.val;")
    }

    @Test
    fun matchesMergeIntoPartition() {
        assertThat(p).matches("merge into dest_tab partition (part1) d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col1 = val;")
    }

    @Test
    fun doesNotMatchMergeIntoInlineViewWithPartitionExtension() {
        assertThat(p).notMatches("merge into (select id, val from dest_tab) "
                + "partition (part1) d "
                + "using source_tab s "
                + "on (d.id = s.id) "
                + "when matched then update set d.val = s.val;")
    }

    @Test
    fun matchesMergeWithDefaultValues() {
        assertThat(p).matches("merge into dest_tab d "
                + "using source_tab s on (d.id = s.id) "
                + "when matched then update set col1 = val, col2 = s.val, col3 = default "
                + "when not matched then insert values (val, s.val, default);")
    }

    @Test
    fun matchesMergeInsertValuesOfCollectionElement() {
        assertThat(p).matches("merge into tab d using src s on (d.id = s.id) " +
                "when not matched then insert values l_tbl(x)(y);")
    }

    @Test
    fun doesNotMatchMergeInsertWithoutValues() {
        assertThat(p).notMatches("merge into tab d using src s on (d.id = s.id) when not matched then insert;")
    }

    @Test
    fun matchesMergeWithoutUsing() {
        assertThat(p).matches("merge into t on (id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a);")
        assertThat(p).matches("merge into t x on (x.id = :i) when matched then update set a = :a;")
        assertThat(p).matches("merge into s.t on (id = :i) when not matched then insert (id, a) values (:i, :a);")
        assertThat(p).matches("merge into t on (id = :i) when not matched then insert (id, a) values (:i, :a) when matched then update set a = :a;")
        assertThat(p).matches("merge /*+ parallel */ into t on (id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a);")
        assertThat(p).matches("merge into t partition (p1) on (id = :i) when matched then update set a = :a;")
        assertThat(p).matches("merge into t on ((id = :i) and a = :a) when matched then update set a = :a;")
        assertThat(p).matches("merge into t on (id = :i) when matched then update set a = :a delete where id = 1 when not matched then insert (id, a) values (:i, :a) where id > 0;")
        assertThat(p).matches("merge into t on (id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a) log errors into e reject limit 5;")
        assertThat(p).matches("merge into t on (id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a);")
    }

    @Test
    fun rejectsMalformedMergeWithoutUsing() {
        assertThat(p).notMatches("merge into t as x on (x.id = :i) when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t when matched then update set a = :a when not matched then insert (id, a) values (:i, :a);")
        assertThat(p).notMatches("merge into t x when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t on (id = :i);")
        assertThat(p).notMatches("merge into t on id = :i when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t on () when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t on (id = :i) using dual when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t using on (id = :i) when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t using when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t using dual when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t x using on (id = :i) when matched then update set a = :a;")
        assertThat(p).notMatches("merge into t on (id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a) when not matched then insert (id, a) values (:i, :a);")
        assertThat(p).notMatches("merge into t on (id = :i) when matched then delete;")
        assertThat(p).notMatches("merge into t on (id = :i) when not matched by source then delete;")
        assertThat(p).notMatches("merge into (select * from t) on (id = :i) when matched then update set a = :a;")
        assertThat(p).notMatches("merge into table(t) on (id = :i) when matched then update set a = :a;")
    }

    @Test
    fun buildsMergeWithoutUsingNodes() {
        val merge = p.parse("merge into t x on (x.id = :i) when matched then update set a = :a when not matched then insert (id, a) values (:i, :a);").getFirstDescendant(DmlGrammar.MERGE_EXPRESSION)!!
        assertThatAst(merge.children.map { it.tokenOriginalValue.lowercase() }.take(4)).containsExactly("merge", "into", "t", "x")
        assertThatAst(merge.hasDirectChildren(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)).isFalse()
        assertThatAst(merge.hasDirectChildren(DmlGrammar.MERGE_UPDATE_CLAUSE, DmlGrammar.MERGE_INSERT_CLAUSE)).isTrue()
        assertThatAst(merge.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenOriginalValue).isEqualTo("x")
    }
}
