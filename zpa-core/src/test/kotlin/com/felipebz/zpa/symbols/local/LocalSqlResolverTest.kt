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
package com.felipebz.zpa.symbols.local

import com.felipebz.zpa.symbols.local.LocalReferenceContext.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Qualified names and calls inside SQL, and single-row `VALUES`, on source shapes observed on Oracle. Each rule is
 * checked as written, with conditional compilation in a region the outcome relies on, and in a region it does not
 * rely on. An outcome decided by the statement's relations relies on no region.
 */
class LocalSqlResolverTest {

    private val support = LocalQualifiedResolverTest()

    private fun steps(source: String, name: String, occurrence: Int = 0, context: LocalReferenceContext? = null) =
        support.steps(source, name, occurrence, context)

    private fun assertSteps(
        template: String, name: String, expected: List<String>, occurrence: Int = 0, context: LocalReferenceContext? = null, fromStep: Int = 0
    ) = support.assertSteps(template, name, expected, occurrence, context, fromStep)

    /** An outcome that relies on no region: unchanged by conditional compilation anywhere. */
    private fun assertRelation(template: String, name: String, occurrence: Int = 0) =
        assertSteps(template, name, listOf("SqlRelation"), occurrence, QUALIFIER, fromStep = 1)

    private val other = "create procedure other is begin /*S*/ null; end;\n/\n"

    @Test
    fun aPlainSqlNameKeepsItsColumnOrLocalReading() {
        val source = "create procedure column_or_local is\n  x number := 2;\n  x2 number := 2;\n  n number;\nbegin\n" +
            "  /*A*/ select count(*) into n from emp where x = x2;\nend;\n/\n" + other
        assertSteps(source, "x", listOf("SqlColumnOrLocal(VARIABLE x@2)"), context = SQL_VALUE)
        assertSteps(source, "x2", listOf("SqlColumnOrLocal(VARIABLE x2@3)"), context = SQL_VALUE)
        // Parameterless names: a local variable named like a SQL function, a local parameterless function.
        assertThat(steps("create procedure sysdate_variable is\n  sysdate date;\n  d date;\nbegin\n  select sysdate into d from dual;\nend;\n/\n", "sysdate", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(VARIABLE sysdate@2)")
        assertThat(steps("create procedure user_variable is\n  user varchar2(10);\n  u varchar2(128);\nbegin\n  select user into u from dual;\nend;\n/\n", "user", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(VARIABLE user@2)")
        assertThat(steps("create procedure sysdate_function is\n  d date;\n  function sysdate return date is begin return null; end;\nbegin\n  select sysdate into d from dual;\nend;\n/\n", "sysdate", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(FUNCTION sysdate@3)")
        assertThat(steps("create procedure select_list is\n  n number := 42;\n  m number;\nbegin\n  select n into m from t8;\nend;\n/\n", "n", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(VARIABLE n@2)")
    }

    @Test
    fun aRelationOfTheStatementQualifiesBeforeAnyLocal() {
        // The table, under a label of the same name.
        assertRelation("create procedure table_qualifier is\n  n number;\nbegin\n  <<emp>>\n  declare\n    x number := 2;\n  begin\n" +
            "    /*A*/ select count(*) into n from emp where emp.x = x;\n  end;\nend;\n/\n" + other, "emp")
        // An alias under a label named like it.
        assertRelation("create procedure label_like_alias is\n  n number;\nbegin\n  <<e>>\n  declare\n    x number := 3;\n  begin\n" +
            "    /*A*/ select count(*) into n from emp e where e.x = 2;\n  end;\nend;\n/\n" + other, "e")
        // A record named like the unaliased table.
        assertRelation("create procedure record_like_table is\n  type r is record (x number);\n  n number;\n  emp r;\nbegin\n" +
            "  /*A*/ select count(*) into n from emp where emp.x = 2;\nend;\n/\n" + other, "emp")
        // An UPDATE target under a label of that name.
        assertRelation("create procedure label_like_target is\nbegin\n  <<emp>>\n  declare\n    x number := 3;\n  begin\n" +
            "    /*A*/ update emp set name = 'm' where emp.x = 2;\n  end;\nend;\n/\n" + other, "emp")
        // An enclosing query's alias in a subquery.
        assertRelation("create procedure correlated is\n  n number;\nbegin\n  <<o>>\n  declare\n    x number := 3;\n  begin\n" +
            "    /*A*/ select count(*) into n from emp o where exists (select 1 from dual where o.x = 2);\n  end;\nend;\n/\n" + other, "o")
        // An object column path under a label named like the alias.
        assertRelation("create procedure object_path is\n  n number;\nbegin\n  <<t>>\n  declare\n    obj o_type := o_type(9);\n  begin\n" +
            "    /*A*/ select t.obj.a into n from obj_table t;\n  end;\nend;\n/\n" + other, "t")
        // schema.table.column, with a label named like the schema.
        assertRelation("create procedure schema_qualified is\n  type r is record (x number);\n  n number;\nbegin\n  <<app_schema>>\n  declare\n" +
            "    emp r;\n  begin\n    /*A*/ select count(*) into n from emp where app_schema.emp.x = 2;\n  end;\nend;\n/\n" + other, "app_schema")
    }

    @Test
    fun aQualifierThatIsNotARelationIsPlsql() {
        // A label, the procedure name, a local record.
        assertSteps(
            "create procedure label_qualified is\n  n number;\nbegin\n  <<blk>>\n  declare\n    x number := 2;\n  begin\n" +
                "    /*A*/ select count(*) into n from emp where emp.x = blk.x;\n  end;\nend;\n/\n" + other,
            "blk", listOf("Bound(LABEL blk@4)", "Bound(VARIABLE x@6)")
        )
        assertSteps(
            "create procedure unit_qualified is\n  x number := 2;\n  n number;\nbegin\n  /*A*/ select count(*) into n from emp where x = unit_qualified.x;\nend;\n/\n" + other,
            "unit_qualified", listOf("Bound(PROCEDURE unit_qualified@1)", "Bound(VARIABLE x@2)")
        )
        assertSteps(
            "create procedure row_qualified is\n  n number;\n  r emp%rowtype;\nbegin\n  /*A*/ select count(*) into n from emp where x = r.x;\nend;\n/\n" + other,
            "r", listOf("Bound(VARIABLE r@3)", "Unsupported(MEMBER_NEEDS_TYPING)")
        )
        // A table that FROM aliases is reachable only through the alias: the record named like it.
        assertSteps(
            "create procedure aliased_table is\n  type r is record (x number);\n  n number;\n  emp r;\nbegin\n" +
                "  /*A*/ select count(*) into n from emp e where emp.x = 2;\nend;\n/\n" + other,
            "emp", listOf("Bound(VARIABLE emp@4)", "Field(RECORD_TYPE r@2.x)")
        )
        // A sequence is not a relation: a record named like it wins.
        assertSteps(
            "create procedure record_like_sequence is\n  type r is record (nextval number);\n  sq r;\n  n number;\nbegin\n" +
                "  /*A*/ select sq.nextval into n from dual;\nend;\n/\n" + other,
            "sq", listOf("Bound(VARIABLE sq@3)", "Field(RECORD_TYPE r@2.nextval)")
        )
        // Without a local of that name a sequence or schema is never bound locally.
        assertThat(steps("create procedure sequence_only is\n  n number;\nbegin\n  select sq.nextval into n from dual;\nend;\n/\n", "sq"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun singleRowValuesArePlsqlButEvaluatedBySql() {
        val source = "create procedure single_row is\n  x number := 99;\n  name varchar2(10) := 'zz';\n" +
            "  function f(a number) return number is begin return a; end;\n  function g return number is begin return 1; end;\nbegin\n" +
            "  /*A*/ insert into emp(x, name) values (x, name);\n  insert into emp(x, name) values (f(1), 'g');\n  insert into emp(x, name) values (g, 'p');\nend;\n/\n" + other
        // No column scope: the locals.
        assertSteps(source, "x", listOf("Bound(VARIABLE x@2)"), context = INSERT_VALUE)
        assertSteps(source, "name", listOf("Bound(VARIABLE name@3)"), context = INSERT_VALUE)
        // A local function, called or parameterless, is still used in SQL (PLS-00231).
        assertSteps(source, "f", listOf("LocalFunctionInSql(FUNCTION f@4)"), context = SQL_CALL)
        assertSteps(source, "g", listOf("LocalFunctionInSql(FUNCTION g@5)"), context = INSERT_VALUE)
        // The SELECT list of the same statement keeps the column reading.
        assertThat(steps("create procedure select_after is\n  x number;\n  n number;\nbegin\n  select x into n from emp where rownum = 1;\nend;\n/\n", "x", context = SQL_VALUE))
            .isEqualTo("SqlColumnOrLocal(VARIABLE x@2)")
    }

    @Test
    fun multiTableValuesSeeTheSubqueryColumns() {
        val source = "create procedure insert_all is\n  x number := 99;\nbegin\n" +
            "  /*A*/ insert all into emp(x, name) values (x, 'h') select x from emp where x = 1;\n" +
            "  insert all into emp(x, name) values (s.x, 'i') select x from emp s where x = 1;\nend;\n/\n" + other
        assertSteps(source, "x", listOf("SqlColumnOrLocal(VARIABLE x@2)"), context = SQL_VALUE)
        // The subquery's alias is not visible there (ORA-00904), and is not local either.
        assertSteps(source, "s", listOf("ProvenAbsent"), context = QUALIFIER)
    }

    @Test
    fun callsInSql() {
        // A local function with arguments: PLS-00231 unless a SQL built-in such as UPPER wins.
        assertSteps(
            "create procedure builtin_name is\n  v varchar2(10);\n  function upper(s varchar2) return varchar2 is begin return 'local'; end;\nbegin\n" +
                "  /*A*/ select upper('a') into v from dual;\nend;\n/\n" + other,
            "upper", listOf("LocalFunctionInSql(FUNCTION upper@3)"), context = SQL_CALL
        )
        // Qualified by the procedure name.
        assertSteps(
            "create procedure qualified_call is\n  n number;\n  function f(a number) return number is begin return a; end;\nbegin\n" +
                "  /*A*/ select qualified_call.f(1) into n from dual;\nend;\n/\n" + other,
            "qualified_call", listOf("Bound(PROCEDURE qualified_call@1)", "LocalFunctionInSql(FUNCTION f@3)")
        )
        // A procedure fails (ORA-03066); an element of a local collection is the local's.
        assertSteps(
            "create procedure procedure_in_sql is\n  n number;\n  procedure pr(a number) is begin null; end;\nbegin\n  /*A*/ select pr(1) into n from dual;\nend;\n/\n" + other,
            "pr", listOf("WrongKind(PROCEDURE pr@3)"), context = SQL_CALL
        )
        assertSteps(
            "create procedure element_in_sql is\n  type nt is table of number;\n  v nt := nt(5);\n  n number;\nbegin\n  /*A*/ select v(1) into n from dual;\nend;\n/\n" + other,
            "v", listOf("SqlBuiltinOrLocal(VARIABLE v@3)"), context = SQL_CALL
        )
        // Through a qualifier the variable binds: a qualified name is never a built-in.
        assertSteps(
            "create procedure qualified_element is\n  n number;\nbegin\n  <<blk>>\n  declare\n    type nt is table of number;\n    v nt := nt(5);\n  begin\n" +
                "    /*A*/ select blk.v(1) into n from dual;\n  end;\nend;\n/\n" + other,
            "blk", listOf("Bound(LABEL blk@4)", "Bound(VARIABLE v@7)")
        )
        // A built-in of the same name wins over a variable or a parameter called with arguments.
        assertSteps(
            "create procedure variable_like_builtin is\n  type nt is table of varchar2(10);\n  upper nt := nt('local');\n  v varchar2(10);\nbegin\n" +
                "  /*A*/ select upper(1) into v from dual;\nend;\n/\n" + other,
            "upper", listOf("SqlBuiltinOrLocal(VARIABLE upper@3)"), context = SQL_CALL
        )
        assertSteps(
            "create procedure parameter_like_builtin is\n  procedure q(upper nt_type) is\n    v varchar2(10);\n  begin\n" +
                "    /*A*/ select upper(1) into v from dual;\n  end;\nbegin\n  null;\nend;\n/\n" + other,
            "upper", listOf("SqlBuiltinOrLocal(PARAMETER upper@2)"), context = SQL_CALL
        )
        // A call the regions do not declare is absent: it cannot be a column.
        assertSteps(
            "create procedure unknown_call is\n  n number;\nbegin\n  /*A*/ select nosuch(1) into n from dual;\nend;\n/\n" + other,
            "nosuch", listOf("ProvenAbsent"), context = SQL_CALL
        )
    }

    @Test
    fun storedFunctionsAreCallableInSql() {
        val spec = "create package stored is\n  function f(a number) return number;\n  function g return number;\n  function upper(s varchar2) return varchar2;\n" +
            "  procedure run;\nend;\n/\n"
        fun body(statement: String) = spec + "create package body stored is\n  function f(a number) return number is begin return a; end;\n" +
            "  function g return number is begin return 7; end;\n  function upper(s varchar2) return varchar2 is begin return 'pkg'; end;\n" +
            "  function h(a number) return number is begin return a; end;\n  procedure run is n number; v varchar2(10);\n  begin\n    /*A*/ $statement\n  end;\nend;\n/\n" + other
        // A specification function, unqualified: accepted, unless a built-in of that name wins (UPPER).
        assertSteps(body("select f(5) into n from dual;"), "f", listOf("SqlBuiltinOrLocal(FUNCTION f@9)"), context = SQL_CALL)
        assertSteps(body("select upper('a') into v from dual;"), "upper", listOf("SqlBuiltinOrLocal(FUNCTION upper@11)"), context = SQL_CALL)
        // Qualified by the package name it is the package's function.
        assertSteps(body("select stored.f(6) into n from dual;"), "stored", listOf("Bound(PACKAGE_BODY stored@8)", "OverloadSet(FUNCTION f@9)"))
        // Parameterless, in single-row VALUES and in a SELECT list.
        assertSteps(body("insert into emp(x, name) values (g, 'c');"), "g", listOf("OverloadSet(FUNCTION g@10)"), context = INSERT_VALUE)
        assertSteps(body("select g into n from dual;"), "g", listOf("SqlColumnOrLocal(FUNCTION g@10)"), context = SQL_VALUE)
        // A function private to the body is not stored (PLS-00231).
        assertSteps(body("select h(1) into n from dual;"), "h", listOf("LocalFunctionInSql(FUNCTION h@12)"), context = SQL_CALL)
        // Without the specification in the file public and private cannot be told apart.
        val lone = "create package body lone is\n  function f(a number) return number is begin return a; end;\n" +
            "  procedure run is n number; begin select f(1) into n from dual; end;\nend;\n/\n"
        assertThat(steps(lone, "f", context = SQL_CALL)).isEqualTo("Incomplete[SPECIFICATION_NOT_IN_FILE](LocalFunctionInSql(FUNCTION f@2))")
    }

    @Test
    fun objectTypeMethodsAreCallableInSql() {
        val source = "create type sql_methods as object (x number, member function f return number, member function g return number);\n/\n" +
            "create type body sql_methods as\n  member function f return number is begin return x; end;\n" +
            "  member function g return number is n number; begin /*A*/ select f() + self.f() into n from dual; return n; end;\nend;\n/\n" + other
        // Unqualified: compiles (ORA-03001 only at run time).
        assertSteps(source, "f", listOf("SqlBuiltinOrLocal(FUNCTION f@4)"), context = SQL_CALL)
        assertSteps(source, "self", listOf("ImplicitSelf(FUNCTION g@5)", "OverloadSet(FUNCTION f@4)"))
    }

    @Test
    fun inlineViewsAndInsertQueriesDoNotSeeTheEnclosingRelations() {
        // An inline view in FROM does not see its sibling alias: under a label of that name, the label's variable.
        assertSteps(
            "create procedure inline_view is\n  n number;\nbegin\n  <<e>>\n  declare\n    x number := 3;\n  begin\n" +
                "    /*A*/ select count(*) into n from emp e, (select e.x y from dual) v where v.y = 3;\n  end;\nend;\n/\n" + other,
            "e", listOf("Bound(LABEL e@4)", "Bound(VARIABLE x@6)")
        )
        // Without the label the name is not local (ORA-00904).
        assertThat(steps("create procedure inline_view_alone is\n  n number;\nbegin\n  select count(*) into n from emp e, (select e.x y from dual) v;\nend;\n/\n", "e"))
            .isEqualTo("ProvenAbsent")
        // LATERAL and CROSS APPLY see it.
        assertRelation("create procedure lateral_view is\n  n number;\nbegin\n  <<e>>\n  declare\n    x number := 3;\n  begin\n" +
            "    /*A*/ select count(*) into n from emp e, lateral (select e.x y from dual) v where v.y = e.x;\n  end;\nend;\n/\n" + other, "e")
        assertRelation("create procedure apply_view is\n  n number;\nbegin\n  <<e>>\n  declare\n    x number := 3;\n  begin\n" +
            "    /*A*/ select count(*) into n from emp e cross apply (select e.x y from dual) v where v.y = e.x;\n  end;\nend;\n/\n" + other, "e")
        // The query of INSERT … SELECT does not see the target.
        assertSteps(
            "create procedure insert_select is\nbegin\n  <<emp>>\n  declare\n    x number := 7;\n  begin\n" +
                "    /*A*/ insert into emp (x, name) select emp.x, 'd' from dual;\n  end;\nend;\n/\n" + other,
            "emp", listOf("Bound(LABEL emp@3)", "Bound(VARIABLE x@5)")
        )
        assertThat(steps("create procedure insert_select_alone is\nbegin\n  insert into emp (x, name) select emp.x, 'e' from dual;\nend;\n/\n", "emp"))
            .isEqualTo("ProvenAbsent")
    }

    @Test
    fun rowSourceFunctionsGiveRelations() {
        val analysis = LocalResolverTest().analyze(
            "create procedure row_sources is\n  n number;\nbegin\n  select count(*) into n from t, json_table(t.doc, '$' columns (a number path '$.a')) jt where jt.a = 1;\n" +
                "  select count(*) into n from t, xmltable('/r' passing t.x columns b number path 'b') xt where xt.b = 1;\n" +
                "  select count(*) into n from table(coll) tc where tc.column_value = 1;\nend;\n/\n"
        )
        fun relations(name: String) = LocalResolverTest().reference(analysis, name, 0, QUALIFIER).sql?.relations?.map { it.lookupName }
        assertThat(relations("jt")).containsExactly("T", "JT")
        assertThat(relations("xt")).containsExactly("T", "XT")
        assertThat(relations("tc")).containsExactly("TC")
    }

    @Test
    fun sqlOutcomesAreDeterministic() {
        val source = "create procedure p is\n  type r is record (x number);\n  emp r;\n  n number;\n  function f(a number) return number is begin return a; end;\nbegin\n" +
            "  select count(*) into n from emp e where e.x = emp.x and f(1) = 1 and exists (select 1 from dual d where e.x = d.dummy);\n" +
            "  insert into emp values (n, f(2));\nend;\n/\n"
        val analysis = LocalResolverTest().analyze(source)
        fun all(order: (List<LocalReference>) -> List<LocalReference>): List<String> {
            val outcomes = order(analysis.references).associateWith { LocalResolver.resolveQualified(analysis.model, it) }
            return analysis.references.map { "$it -> ${outcomes.getValue(it).joinToString(" / ") { o -> LocalResolverTest().render(analysis.model, o) }}" }
        }
        val forward = all { it }
        assertThat(all { it.reversed() }).isEqualTo(forward)
        assertThat(forward).anyMatch { it.contains("QUALIFIER e.x") && it.endsWith("SqlRelation") }
            .anyMatch { it.endsWith("Field(RECORD_TYPE r@2.x)") }
            .anyMatch { it.contains("SQL_CALL f") && it.endsWith("LocalFunctionInSql(FUNCTION f@5)") }
            .anyMatch { it.contains("INSERT_VALUE n") && it.endsWith("Bound(VARIABLE n@4)") }
    }
}
