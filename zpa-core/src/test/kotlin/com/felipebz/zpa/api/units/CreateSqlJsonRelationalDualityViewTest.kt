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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import com.felipebz.zpa.api.SingleRowSqlFunctionsGrammar
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateSqlJsonRelationalDualityViewTest : RuleTest() {

    private val root = "select json {'_id': o.oid, 'st': o.st}"
    private val child = "(select json {'cid': c.cid} from c c where c.cid = o.cid)"

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
    }

    private fun view(body: String) = "create json relational duality view zv as $body;"

    private fun matches(vararg bodies: String) {
        for (body in bodies) assertThat(p).describedAs(body).matches(view(body))
    }

    private fun notMatches(vararg bodies: String) {
        for (body in bodies) assertThat(p).describedAs(body).notMatches(view(body))
    }

    @Test
    fun matchesBasicDefinitions() {
        matches(
            "$root from o o", "$root from o", "$root from s.o o", "($root from o o)",
            "select json {'_id': o.oid, 'items': [select json {'iid': i.iid, 'q': i.q} from i i where i.oid = o.oid]} from o o",
            "select json {'_id': o.oid, 'c': $child} from o o",
            "select json {'_id': o.oid, 'c': $child, 'items': [select json {'iid': i.iid} from i i where i.oid = o.oid]} from o o",
            "select json {'_id' is o.oid, 'st' value o.st, key 's' is o.st} from o o",
            "select json {'_id': o.oid, o.st, 'x': 1, 'y': upper(o.st), 'z': o.st || 'a'} from o o",
            "select json {'_id': o.oid, 'st': o.st format json} from o o",
            "select json_object('_id' value o.oid) from o o",
            "select json {'_id': o.oid, 'c': (select json_arrayagg(json {'cid': c.cid}) from c c where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, 'c': (select c.cid from c c where c.cid = o.cid)} from o o"
        )
    }

    @Test
    fun matchesHeaderVariants() {
        for (header in listOf(
            "create or replace json relational duality view zv", "create force json relational duality view zv",
            "create nonEditionable json duality view zv", "create json relational duality view if not exists zv",
            "create json relational duality view zv (data)", "create json relational duality view zv enable logical replication"
        )) assertThat(p).describedAs(header).matches("$header as $root from o o;")
    }

    @Test
    fun matchesTableAnnotations() {
        matches(
            "$root from o o with insert", "$root from o o with noinsert", "$root from o o with update", "$root from o o with noupdate",
            "$root from o o with delete", "$root from o o with nodelete", "$root from o o with check", "$root from o o with nocheck",
            "$root from o o with check etag", "$root from o o with nocheck etag", "$root from o with insert",
            "$root from o o with insert update delete", "$root from o o with update insert",
            "$root from o o with insert check etag delete",
            // Oracle reports conflicting and repeated tags only after parsing (ORA-40934, ORA-40947).
            "$root from o o with insert noinsert", "$root from o o with check nocheck", "$root from o o with insert insert",
            "$root from o o with insert where o.oid > 1 with check option",
            "$root from o o where o.oid > 1 with check option", "$root from o o where o.oid > 1 with read only",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from c c with insert nocheck where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from c c where c.cid = o.cid with check option)} from o o"
        )
    }

    @Test
    fun matchesFieldAnnotations() {
        matches(
            "select json {'_id': o.oid, 'st': o.st with check} from o o", "select json {'_id': o.oid, 'st': o.st with nocheck} from o o",
            "select json {'_id': o.oid, 'st': o.st with check etag} from o o", "select json {'_id': o.oid, 'st': o.st with nocheck etag} from o o",
            "select json {'_id': o.oid, 'st': o.st with update} from o o", "select json {'_id': o.oid, 'st': o.st with noupdate} from o o",
            "select json {'_id': o.oid, 'st': o.st with check update} from o o",
            "select json {'_id': o.oid, 'st': o.st with update check etag} from o o",
            "select json {'_id': o.oid with noupdate, 'st': o.st with nocheck} from o o",
            "select json {'_id': o.oid, o.st with update} from o o",
            "select json {'_id': o.oid, key 'st' is o.st with update} from o o",
            "select json {'_id': o.oid, 'st': o.st format json with update} from o o",
            "select json {'_id': o.oid, 'st': upper(o.st) with update} from o o",
            "select json_object('_id' value o.oid, 'st' value o.st with update) from o o",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid with noupdate} from c c with insert where c.cid = o.cid)} from o o",
            // Reported by Oracle only after parsing (ORA-40934, ORA-40947).
            "select json {'_id': o.oid, 'st': o.st with update noupdate} from o o",
            "select json {'_id': o.oid, 'st': o.st with check nocheck} from o o",
            "select json {'_id': o.oid, 'st': o.st with update update} from o o"
        )
    }

    @Test
    fun matchesUnnestAndFlexEntries() {
        matches(
            "select json {'_id': o.oid, unnest (select json {'cid': c.cid} from c c where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, unnest (select json {'cid': c.cid} from c c with update where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, unnest (select json {'cid': c.cid} from c c where c.cid = o.cid) with update} from o o",
            "select json {'_id': o.oid, o.dt as flex} from o o", "select json {'_id': o.oid, o.dt as flex column, 'st': o.st} from o o",
            "select json {'_id': o.oid, o.dt as flex with update} from o o", "select json {'_id': o.oid, o.a as flex, o.b as flex} from o o"
        )
    }

    @Test
    fun rejectsMalformedDefinitions() {
        notMatches(
            "$root from", "select json {'_id': o.oid,} from o o", "select json {'_id': o.oid from o o",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from c c where c.cid = o.cid]} from o o",
            "select json {'_id': o.oid, 'c': [select json {'cid': c.cid} from c c where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from c c where)} from o o",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from where c.cid = o.cid)} from o o",
            "select json {key '_id' : o.oid} from o o"
        )
    }

    @Test
    fun rejectsMalformedTableAnnotations() {
        notMatches(
            "$root from o o with", "$root from o o with foo", "$root from o o with insert, update", "$root from o o with etag",
            "$root from o o with etag check", "$root from o o with check etag etag", "$root from o o with insert with update",
            "$root from o o with check option", "$root from o o with read only", "$root from o o where o.oid > 1 with insert",
            "$root from o o where o.oid > 1 with check option with read only",
            "select json {'_id': o.oid, 'c': (select json {'cid': c.cid} from c c where c.cid = o.cid with update)} from o o"
        )
    }

    @Test
    fun rejectsMalformedFieldAnnotations() {
        notMatches(
            "select json {'_id': o.oid, 'st': o.st with} from o o", "select json {'_id': o.oid, 'st': o.st with insert} from o o",
            "select json {'_id': o.oid, 'st': o.st with delete} from o o", "select json {'_id': o.oid, 'st': o.st with noinsert} from o o",
            "select json {'_id': o.oid, 'st': o.st with check with update} from o o",
            "select json {'_id': o.oid, 'st': o.st with update format json} from o o",
            "select json {'_id': o.oid, 'st': o.st with etag} from o o"
        )
    }

    @Test
    fun rejectsMalformedUnnestAndFlexEntries() {
        notMatches(
            "select json {'_id': o.oid, 'c': unnest (select json {'cid': c.cid} from c c where c.cid = o.cid)} from o o",
            "select json {'_id': o.oid, unnest select json {'cid': c.cid} from c c where c.cid = o.cid} from o o",
            "select json {'_id': o.oid, unnest [select json {'cid': c.cid} from c c where c.cid = o.cid]} from o o",
            "select json {'_id': o.oid, o.dt as flex1} from o o", "select json {'_id': o.oid, as flex} from o o",
            "select json {'_id': o.oid, o.dt flex} from o o", "select json {'_id': o.oid, 'x': o.dt as flex} from o o"
        )
    }

    @Test
    fun keepsAnnotationsOutOfOrdinarySql() {
        for (sql in listOf(
            "select json {'a': t.x with update} from t;", "select json {'a': t.x with nocheck} from t;",
            "select json {'a': t.x} from t with insert;", "select json {'a': t.x} from t with check;",
            "select json {'a': t.x} from t where t.y = 1 with check option;",
            "select json {'a': t.x, t.y as flex} from t;", "select json {'a': t.x, unnest (select 1 from d)} from t;",
            "create view v as select json {'a': t.x with update} from t;", "create view v as select t.x from t with insert;",
            "create materialized view m as select json {'a': t.x} from t with insert;"
        )) assertThat(p).describedAs(sql).notMatches(sql)
        assertThat(p).matches("select json {'a': t.x with typename} from t;")
        assertThat(p).matches("create view v as select t.x from t where t.y = 1 with check option;")
        assertThat(p).matches("select unnest from t where unnest = 1;")
    }

    @Test
    fun buildsAnnotationNodes() {
        val tree = p.parse(view(
            "select json {'_id': o.oid with noupdate, o.dt as flex, unnest (select json {'cid': c.cid} from c c where c.cid = o.cid)} " +
                "from o o with insert check etag where o.oid > 1 with check option"))
        val duality = tree.getFirstDescendant(DdlGrammar.CREATE_JSON_RELATIONAL_DUALITY_VIEW)!!
        assertThatAst(duality.children.map { it.name }).containsExactly(
            "CREATE", "JSON", "RELATIONAL", "DUALITY", "VIEW", "UNIT_NAME", "AS", "SELECT_EXPRESSION", "SEMICOLON")
        val entries = duality.getFirstDescendant(SingleRowSqlFunctionsGrammar.JSON_OBJECT_EXPRESSION)!!
            .getDescendants(SingleRowSqlFunctionsGrammar.JSON_OBJECT_ENTRY)
        assertThatAst(entries.map { e -> e.children.map { it.name } }.take(3)).containsExactly(
            listOf("LITERAL", "COLON", "MEMBER_EXPRESSION", "DUALITY_VIEW_COLUMN_TAGS"),
            listOf("MEMBER_EXPRESSION", "AS", "FLEX"),
            listOf("UNNEST", "LPARENTHESIS", "SELECT_EXPRESSION", "RPARENTHESIS"))
        val tags = duality.getFirstDescendant(DdlGrammar.DUALITY_VIEW_COLUMN_TAGS)!!
        assertThatAst(tags.children.map { it.name }).containsExactly("WITH", "NOUPDATE")
        val query = duality.getFirstChild(DmlGrammar.SELECT_EXPRESSION).getFirstChild(DmlGrammar.QUERY_BLOCK)
        val table = query.getFirstChild(DmlGrammar.FROM_CLAUSE).getFirstDescendant(DdlGrammar.DUALITY_VIEW_TABLE_TAGS)!!
        assertThatAst(table.children.map { it.name }).containsExactly("WITH", "INSERT", "CHECK", "ETAG")
        assertThatAst(query.children.map { it.name }).containsExactly(
            "SELECT", "SELECT_COLUMN", "FROM_CLAUSE", "WHERE_CLAUSE", "WITH", "CHECK", "OPTION")
    }
}
