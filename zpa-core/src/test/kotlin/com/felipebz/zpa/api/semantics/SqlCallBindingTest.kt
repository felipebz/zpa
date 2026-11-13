//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The decision of the local analysis for calls written inside static SQL, with SQL's name resolution. */
class SqlCallBindingTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val tax = "create package pk_tax as\n  function rate(p_code varchar2) return number;\n  function rate(p_code number) return number;\nend;\n/\n"

    private val conv = "create function fn_conv(p number) return number is begin return p; end;\n/\n"

    private val bill = "create procedure p_bill is\n" +
        "  v_total number;\n" +
        "  function local_fee(p number) return number is begin return p; end;\n" +
        "  function upper(p varchar2) return varchar2 is begin return p; end;\n" +
        "begin\n" +
        "  select sum(amount * pk_tax.rate(code)) into v_total from bill_line;\n" +
        "  select count(*) into v_total from bill_line where amount > pk_tax.rate('A');\n" +
        "  insert into bill_log (total, rate) values (v_total, pk_tax.rate('X'));\n" +
        "  update bill_log set rate = pk_tax.rate(1) where total = pk_tax.rate(v_total);\n" +
        "  forall i in 1 .. 2 insert into bill_log (rate) values (pk_tax.rate(i));\n" +
        "  select pk_tax.rate(1) into v_total from bill_line pk_tax;\n" +
        "  select b.addr.city_of(1) into v_total from bill_line b;\n" +
        "  select fn_conv(amount) into v_total from bill_line where rownum = 1;\n" +
        "  select local_fee(amount) into v_total from bill_line where rownum = 1;\n" +
        "  select upper('a') into v_total from dual;\n" +
        "  declare\n" +
        "    qty_of number;\n" +
        "  begin\n" +
        "    select qty_of(1) into v_total from dual;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val fee = "create package pk_fee as\n  function fee(p number) return number;\n  function caller return number;\nend;\n/\n" +
        "create package body pk_fee as\n" +
        "  function hidden(p number) return number is begin return p; end;\n" +
        "  function fee(p number) return number is begin return p; end;\n" +
        "  function caller return number is\n    v number;\n  begin\n" +
        "    select fee(1) + hidden(1) into v from dual;\n" +
        "    return v;\n  end;\nend;\n/\n"

    private val rateSpec = "create package pk_rate as\n  function rate_of(p number) return number;\n  function caller return number;\nend;\n/\n"

    private val rateBody = "create package body pk_rate as\n" +
        "  function rate_of(p number) return number is begin return p; end;\n" +
        "  function private_of(p number) return number is begin return p; end;\n" +
        "  function caller return number is\n    v number;\n  begin\n" +
        "    select rate_of(1) + private_of(1) into v from dual;\n" +
        "    return v;\n  end;\nend;\n/\n"

    private val lost = "create package body pk_lost as\n  function f(p number) return number is begin return p; end;\n" +
        "  procedure run is\n    v number;\n  begin\n    select f(1) into v from dual;\n  end;\nend;\n/\n"

    private val loop = "create procedure p_loop is\n" +
        "  v_total number;\n" +
        "  v_code varchar2(10);\n" +
        "  v_name varchar2(30);\n" +
        "begin\n" +
        "  for i in 1 .. 2 loop\n" +
        "    select pk_tax.rate(i) into v_total from dual;\n" +
        "    v_total := pk_tax.rate(i);\n" +
        "  end loop;\n" +
        "  select pk_tax.rate(v_code) into v_total from dual;\n" +
        "  insert into bill_log (rate) values (pk_tax.rate(v_code));\n" +
        "  select pk_tax.rate(code) into v_total from bill_line;\n" +
        "  select pk_tax.rate(dummy) into v_total from dual;\n" +
        "  select p.full_name(1) into v_name from person_tab p;\n" +
        "end;\n/\n"

    private val sources = mapOf("loop.sql" to loop, "lost.pkb" to lost, "tax.pks" to tax, "conv.sql" to conv, "bill.sql" to bill, "fee.sql" to fee, "rate.pks" to rateSpec, "rate.pkb" to rateBody)

    private fun render(binding: CallBinding): String = when (binding) {
        is CallBinding.Project -> {
            val resolution = binding.resolution
            val selected = resolution.selected.map { " (" + it.declaration.parameters.joinToString { p -> "${p.name} ${p.typeRef.name}" } + ")" }.orElse("")
            "Project(${resolution.status}$selected of ${resolution.structural.candidates.size})"
        }
        is CallBinding.Local -> "Local(${binding.status} ${binding.selected})"
        else -> binding.toString()
    }

    /** `line:name=call|name` for each call name of [file] (the first child of a `METHOD_CALL`). */
    private fun calls(file: String): List<String> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.METHOD_CALL).map { it.firstChild }
                    .filter { it.type === PlSqlGrammar.VARIABLE_NAME || it.type === PlSqlGrammar.MEMBER_EXPRESSION }
                    .forEach { name ->
                        rows += "${name.token.line}:${name.tokens.joinToString("") { it.originalValue }}=${render(this.context.callBinding(name))}" +
                            "|${this.context.nameBinding(name)}"
                    }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    @Test
    fun callsInsideSql() {
        assertThat(calls("bill.sql")).containsExactly(
            // SELECT list: a column may take the actual, so its type narrows nothing.
            "6:sum=Unavailable(NOT_A_COVERED_NAME)|Unavailable(NOT_A_COVERED_NAME)",
            "6:pk_tax.rate=Project(AMBIGUOUS of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "7:count=Unavailable(NOT_A_COVERED_NAME)|Unavailable(NOT_A_COVERED_NAME)",
            // WHERE, the VALUES of an INSERT, the SET of an UPDATE, and an INSERT in FORALL.
            "7:pk_tax.rate=Project(RESOLVED (p_code varchar2) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "8:pk_tax.rate=Project(RESOLVED (p_code varchar2) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "9:pk_tax.rate=Project(RESOLVED (p_code number) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            // A column of bill_log may take v_total.
            "9:pk_tax.rate=Project(AMBIGUOUS of 2)|Unavailable(NOT_A_COVERED_NAME)",
            // The index of FORALL is a PLS_INTEGER, and no column takes a name in the VALUES of an INSERT.
            "10:pk_tax.rate=Project(RESOLVED (p_code number) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            // An alias named like a package of the project: the method of an object table's rows, else the package's function.
            "11:pk_tax.rate=KeptLocal(SQL_RELATION_OR_PROJECT)|Unavailable(NOT_A_COVERED_NAME)",
            // `alias.column.method(x)`: the relation takes it.
            "12:b.addr.city_of=KeptLocal(SQL_RELATION)|Unavailable(NOT_A_COVERED_NAME)",
            // A standalone function of another file, called unqualified.
            "13:fn_conv=Project(RESOLVED (p number) of 1)|Unavailable(NOT_A_COVERED_NAME)",
            // A function of the procedure: Oracle rejects it in SQL (PLS-00231).
            "14:local_fee=KeptLocal(NOT_CALLABLE_IN_SQL)|Unavailable(NOT_A_COVERED_NAME)",
            // A SQL built-in takes the name of a local function.
            "15:upper=Unavailable(NOT_A_COVERED_NAME)|Unavailable(NOT_A_COVERED_NAME)",
            // A local variable takes the name of a schema function: Oracle rejects the call (PLS-00222).
            "19:qty_of=KeptLocal(LOCAL_CAPTURE)|Local(VARIABLE@17:4)"
        )
    }

    @Test
    fun actualsNoColumnCanTakeAndMethodsOfARelation() {
        assertThat(calls("loop.sql")).containsExactly(
            // Over DUAL alone the loop index and the variables are the local ones, typed as in PL/SQL.
            "7:pk_tax.rate=Project(RESOLVED (p_code number) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "8:pk_tax.rate=Project(RESOLVED (p_code number) of 2)|NothingLocal",
            "10:pk_tax.rate=Project(RESOLVED (p_code varchar2) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "11:pk_tax.rate=Project(RESOLVED (p_code varchar2) of 2)|Unavailable(NOT_A_COVERED_NAME)",
            // A column may take the actual: nothing narrows.
            "12:pk_tax.rate=Project(AMBIGUOUS of 2)|Unavailable(NOT_A_COVERED_NAME)",
            "13:pk_tax.rate=Project(AMBIGUOUS of 2)|Unavailable(NOT_A_COVERED_NAME)",
            // No function p.full_name in the project: the alias takes it, the method of an object table's rows.
            "14:p.full_name=KeptLocal(SQL_RELATION)|Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun functionsOfAPackageCalledInSqlFromItsBody() {
        assertThat(calls("fee.sql")).containsExactly(
            // A function of the specification runs (its body definition); one private to the body is rejected (PLS-00231).
            "12:fee=Local(RESOLVED FUNCTION@8:11)|Unavailable(NOT_A_COVERED_NAME)",
            "12:hidden=KeptLocal(NOT_CALLABLE_IN_SQL)|Unavailable(NOT_A_COVERED_NAME)"
        )
        // The specification in another file.
        assertThat(calls("rate.pkb")).containsExactly(
            "7:rate_of=Project(RESOLVED (p number) of 1)|Unavailable(NOT_A_COVERED_NAME)",
            "7:private_of=KeptLocal(NOT_CALLABLE_IN_SQL)|Unavailable(NOT_A_COVERED_NAME)"
        )
        // The specification is not in the project: whether the function is callable in SQL is not decided.
        assertThat(calls("lost.pkb")).containsExactly("6:f=KeptLocal(INCOMPLETE)|Unavailable(NOT_A_COVERED_NAME)")
    }
}
