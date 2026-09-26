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
package com.felipebz.zpa.api.sql

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraphTableTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DmlGrammar.SELECT_EXPRESSION)
    }

    private fun graph(body: String) = "select * from graph_table (g $body)"

    @Test
    fun matchesGraphReferences() {
        assertThat(p).matches(graph("match (a) columns (a.name)"))
        assertThat(p).matches("select * from graph_table (scott.g match (a) columns (a.*))")
        assertThat(p).matches(
            "select * from graph_table (s.g as of timestamp (systimestamp - interval '2' minute) " +
                "match (a) columns (a.*))")
        assertThat(p).matches("select * from graph_table (g as of scn 1234 match (a) columns (a.name))")
        assertThat(p).matches(
            "select * from graph_table (g as of period for valid_time sysdate match (a) columns (a.name))")
    }

    @Test
    fun matchesVertexPatterns() {
        assertThat(p).matches(graph("match () columns (1 as dummy)"))
        assertThat(p).matches(graph("match (a is person) columns (a.name)"))
        assertThat(p).matches(graph("match (is person|university) columns (1 as dummy)"))
        assertThat(p).matches(graph("match (\"b\" is \"Person Label\"|\"Other\") columns (\"b\".name)"))
        assertThat(p).matches(graph("match (a where a.dob > date '1980-01-01') columns (a.name)"))
        assertThat(p).matches(graph("match (a is person where a.name = 'John' and a.id <> 1) columns (a.name)"))
    }

    @Test
    fun matchesEdgePatternsInEveryDirection() {
        assertThat(p).matches(graph("match (a) -[e is friends]-> (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) <-[e is friends]- (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) -[e is friends]- (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) <-[e]-> (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) -[is friends where e.d > date '2001-01-01']- (b) columns (a.name)"))
        assertThat(p).matches(graph("match -> columns (1 as dummy)"))
        assertThat(p).matches(graph("match -[e]-> columns (e.id)"))
        assertThat(p).matches(graph("match (a) <- (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) - (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a) <-> (b) columns (a.name)"))
        // Oracle 26 accepts whitespace inside the arrows.
        assertThat(p).matches(graph("match (a) - [e] - > (b) columns (a.name)"))
        assertThat(p).matches(graph("match (a)-[is friend_of]->{1,10}(b) columns (a.id as aid)"))
    }

    @Test
    fun matchesPathConcatenationAndLists() {
        assertThat(p).matches(graph("match (n) -[is f]- () -[is f]- (m) columns (m.name)"))
        assertThat(p).matches(graph("match (n) -[is f]- -[is f]- (m) columns (m.name)"))
        assertThat(p).matches(graph(
            "match (a) <-[e1]- (b), (b) <-[e2]- (c), (c) <-[e3]- (a) where a.name = 'Mary' " +
                "columns (a.name as person_a, b.name as person_b)"))
        assertThat(p).matches(graph(
            "match path1 = (u1) <-[is s]- (p1), path2 = (p1) -[is f]-{1,2} (p2) columns (p2.name)"))
    }

    @Test
    fun matchesQuantifiedPaths() {
        assertThat(p).matches(graph("match (a) -[e]-{2} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) -[e]-{1,3} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) -[e]-{,3} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) -{1,2} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) (-[is friends]-){2} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) ((x) -[e]- (y) where x.id < y.id){1,3} (b) columns (b.name)"))
        assertThat(p).matches(graph("match (a) (-[e]- (x) where x.h > a.h) {,3} (b) columns (b.name)"))
        assertThat(p).matches(graph("match ((a) -[e]-> (b)){1,2} columns (1 as x)"))
    }

    @Test
    fun matchesGraphTableShape() {
        assertThat(p).matches(graph("match (a) one row per match columns (a.name)"))
        assertThat(p).matches(graph(
            "match (a) -[e]-{1,2} (b) one row per vertex (v) " +
                "columns (matchnum() as m, element_number(v) as n, v.name)"))
        assertThat(p).matches(graph(
            "match p = (a) -[e]->{1,2} (b), q = (b) one row per vertex (v) in (p, q) " +
                "columns (path_name() as pn, v.*)"))
        assertThat(p).matches(graph(
            "match (a) -[e]->{0,3} (is person) one row per step (src, e2, dst) " +
                "columns (listagg(e.id, ', ') as ids, src.name as src_name, e2.id, dst.*)"))
        assertThat(p).matches(graph("match (n) columns (n.name, n.height * 3.281 as height_in_feet)"))
    }

    @Test
    fun matchesGraphValueExpressions() {
        assertThat(p).matches(graph(
            "match (p1) -[e1]- (p2) -[e2]- (p3) " +
                "where p1.name = 'John' and ((p1 is source of e1 and p2 is not source of e2) or " +
                "(p1 is destination of e1 and p2 is not destination of e2)) " +
                "columns (case when p1 is source of e1 then 'Out' else 'In' end as d)"))
        assertThat(p).matches(graph(
            "match (x is person|university) " +
                "columns (case when x is labeled person then 1 when x is not labeled \"U\" then 2 end as l, " +
                "property_exists(x, dob) as has_dob, vertex_id(x) as id)"))
        assertThat(p).matches(graph(
            "match (p) -[e]-{2,5} (f) where count(edge_id(e)) = count(distinct edge_id(e)) " +
                "and not vertex_equal(p, f) " +
                "columns (json_arrayagg(case when e.s is not null then e.s else cast(e.id as varchar(100)) end) as path)"))
        assertThat(p).matches(graph("match (n) where n.data.department = 'HR' columns (n.data.role.string() as role)"))
    }

    @Test
    fun graphTableComposesWithOrdinaryFromItems() {
        assertThat(p).matches("select gt.name from graph_table (g match (a) columns (a.name)) gt order by gt.name")
        assertThat(p).matches("select * from graph_table (g match (a) columns (a.id)) gt, other_table t where gt.id = t.id")
        assertThat(p).matches("select * from graph_table (g match (a) columns (a.id)) gt join t on gt.id = t.id")
        assertThat(p).matches("select * from t join graph_table (g match (a) columns (a.id)) gt on gt.id = t.id")
    }

    @Test
    fun keepsOrdinaryTableFunctionsAndReferences() {
        assertThat(p).matches("select * from graph_table")
        assertThat(p).matches("select * from graph_table gt")
        assertThat(p).matches("select * from my_graph_table(1)")
        assertThat(p).matches("select * from pkg.graph_table(1)")
        assertThat(p).matches("select * from table(graph_table(1))")
        assertThat(p).matches("select graph_table(1) from dual")
    }

    @Test
    fun rejectsGraphTableAsGenericTableFunction() {
        // Oracle parses `graph_table(` in FROM as the operator even when such a function exists.
        assertThat(p).notMatches("select * from graph_table(1)")
        assertThat(p).notMatches("select * from graph_table(g)")
    }

    @Test
    fun rejectsMalformedGraphTable() {
        assertThat(p).notMatches("select * from graph_table ()")
        assertThat(p).notMatches(graph("columns (1)"))
        assertThat(p).notMatches(graph("match columns (1)"))
        assertThat(p).notMatches(graph("match (a)"))
        assertThat(p).notMatches(graph("match (a), columns (a.name)"))
        assertThat(p).notMatches(graph("match (a is) columns (a.name)"))
        assertThat(p).notMatches(graph("match (a is person|) columns (a.name)"))
        assertThat(p).notMatches(graph("match (a) -[e]-{1,} (b) columns (b.name)"))
        assertThat(p).notMatches(graph("match (a) -[e]-{} (b) columns (b.name)"))
        assertThat(p).notMatches(graph("match (a) -[e]-{,} (b) columns (b.name)"))
        assertThat(p).notMatches(graph("match (a) -[e]-{1,2,3} (b) columns (b.name)"))
        assertThat(p).notMatches(graph("match (a) columns ()"))
        assertThat(p).notMatches(graph("match (a) columns (a.name,)"))
        // Oracle 26 requires AS before a GRAPH_TABLE column name (ORA-00907).
        assertThat(p).notMatches(graph("match (a) columns (a.name n)"))
    }

    @Test
    fun rejectsOracleRestrictedPatternForms() {
        // Only edges and parenthesized paths are quantifiable; parenthesized paths must be quantified.
        assertThat(p).notMatches(graph("match (a){2} columns (a.name)"))
        assertThat(p).notMatches(graph("match (a) (-[e]-) (b) columns (a.name)"))
        assertThat(p).notMatches(graph("match ((a)) columns (a.name)"))
        // IS LABELED takes one label (ORA-00996 for a disjunction).
        assertThat(p).notMatches(graph("match (a) where a is labeled person|friends columns (a.name)"))
    }

    @Test
    fun rejectsMalformedRowsClause() {
        assertThat(p).notMatches(graph("match (a) one row per vertex (v, w) columns (v.name)"))
        assertThat(p).notMatches(graph("match (a) one row per step (v, e) columns (v.name)"))
        assertThat(p).notMatches(graph("match (a) one row per vertex () columns (a.name)"))
        assertThat(p).notMatches(graph("match (a) one row per match (v) columns (a.name)"))
        assertThat(p).notMatches(graph("match (a) one row per vertex (v) in () columns (v.name)"))
    }

    @Test
    fun graphPredicatesAreScopedToGraphTable() {
        // Oracle rejects these outside GRAPH_TABLE (ORA-00919).
        assertThat(p).notMatches("select * from dual where a is source of e")
        assertThat(p).notMatches("select * from dual where a is labeled person")
        assertThat(p).matches("select * from dual where a is null")
    }

    @Test
    fun graphTableIsNotADeleteOrUpdateTarget() {
        // Oracle 26 raises ORA-40968 at GRAPH_TABLE before diagnosing a malformed body or
        // trailing tokens, unlike non-updatable views (ORA-01732 only after a full parse).
        setRootRule(DmlGrammar.DELETE_EXPRESSION)
        assertThat(p).notMatches("delete from graph_table (g match (a) columns (a.id))")
        assertThat(p).notMatches("delete graph_table (g match (a) columns (a.id))")
        assertThat(p).matches("delete from (select * from graph_table (g match (a) columns (a.id)))")
        assertThat(p).matches(
            "delete from t where id in (select id from graph_table (g match (a) columns (a.id)))")

        setRootRule(DmlGrammar.UPDATE_EXPRESSION)
        assertThat(p).notMatches("update graph_table (g match (a) columns (a.id)) set id = id")
        assertThat(p).matches(
            "update t set id = id where id in (select id from graph_table (g match (a) columns (a.id)))")

        setRootRule(DmlGrammar.INSERT_EXPRESSION)
        assertThat(p).notMatches("insert into graph_table (g match (a) columns (a.id)) values (1)")
        assertThat(p).matches("insert into t select id from graph_table (g match (a) columns (a.id))")
    }

    @Test
    fun graphTableIsAMergeUsingSource() {
        setRootRule(DmlGrammar.MERGE_EXPRESSION)
        assertThat(p).matches(
            "merge into t using graph_table (g match (a) columns (a.id, a.name)) gt on (t.id = gt.id) " +
                "when matched then update set t.name = gt.name")
        assertThat(p).matches(
            "merge into t using (select * from graph_table (g match (a) columns (a.id))) gt on (t.id = gt.id) " +
                "when not matched then insert (id) values (gt.id)")
        assertThat(p).notMatches(
            "merge into t using graph_table (g match (a) columns ()) gt on (t.id = gt.id) " +
                "when matched then update set t.name = gt.name")
    }

    @Test
    fun graphTableIsAMergeIntoTarget() {
        // Undocumented, but Oracle 26ai parses and executes both branches against the vertex table.
        setRootRule(DmlGrammar.MERGE_EXPRESSION)
        assertThat(p).matches(
            "merge into graph_table (g match (a) columns (a.id, a.name)) gt using src s on (gt.id = s.id) " +
                "when matched then update set gt.name = s.name")
        assertThat(p).matches(
            "merge into graph_table (g match (a) columns (a.id, a.name)) gt using src s on (gt.id = s.id) " +
                "when not matched then insert (id, name) values (s.id, s.name)")
        assertThat(p).matches(
            "merge into graph_table (g match (a) columns (a.id, a.name)) using src s on (id = s.id) " +
                "when matched then update set name = s.name")
        assertThat(p).matches("merge into graph_table gt using src s on (gt.id = s.id) when matched then update set gt.name = s.name")
    }

    @Test
    fun rejectsMalformedMergeIntoGraphTable() {
        setRootRule(DmlGrammar.MERGE_EXPRESSION)
        assertThat(p).notMatches(
            "merge into graph_table (g match (a) columns ()) gt using src s on (gt.id = s.id) " +
                "when matched then update set gt.name = s.name")
        assertThat(p).notMatches(
            "merge into graph_table (g) gt using src s on (1 = 1) when matched then update set gt.name = s.name")
        // Oracle 26ai rejects both with ORA-38107.
        assertThat(p).notMatches(
            "merge into graph_table (g match (a) columns (a.id)) as gt using src s on (gt.id = s.id) " +
                "when matched then update set gt.name = s.name")
        assertThat(p).notMatches(
            "merge into graph_table (g match (a) columns (a.id)) partition (p1) gt using src s on (gt.id = s.id) " +
                "when matched then update set gt.name = s.name")
    }
}
