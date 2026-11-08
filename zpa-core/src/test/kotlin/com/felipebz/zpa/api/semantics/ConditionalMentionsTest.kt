//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Undecided conditional-compilation text changes the answers about a name only when it writes the name, or when it
 * may change the block structure; the use of a name inside a kept branch stays undecided whatever it names.
 */
class ConditionalMentionsTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Runs [check] over [text] as the file `u.sql`, with no project. */
    private fun walk(text: String, check: PlSqlCheck) {
        val project = ProjectAnalysisContext.NOT_PREPARED
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
    }

    /** `line:NAME=binding` of each plain name of [text]. */
    private fun bindings(text: String): List<String> {
        val rows = ArrayList<String>()
        walk(text, object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME).forEach { rows += "${it.token.line}:${it.tokenValue}=${context.nameBinding(it)}" }
            }
        })
        return rows
    }

    /** `NAME: evidence` of each declaration of the [types] of [text], by [evidence]. */
    private fun declarations(text: String, vararg types: PlSqlGrammar, evidence: PlSqlVisitorContext.(AstNode) -> Any): List<String> {
        val rows = ArrayList<String>()
        walk(text, object : PlSqlCheck() {
            init {
                subscribeTo(*types)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.evidence(node)}"
            }
        })
        return rows
    }

    private fun usages(text: String, vararg types: PlSqlGrammar) = declarations(text, *types) { usageEvidence(it) }

    // A package body whose `g_total` is read in `p2`; [conditional] is written among the body's declarations.
    private fun body(conditional: String) = "create or replace package pk_m is\n  procedure p2;\nend;\n/\n" +
        "create or replace package body pk_m is\n  g_total number;\n  $conditional\n" +
        "  procedure p2 is\n    n number;\n  begin\n    n := g_total;\n  end;\nend;\n/\n"

    private val debug = "\$IF \$\$debug \$THEN procedure dbg is begin dbms_output.put_line('x'); end;"

    /** The binding of the read of `g_total` in `p2`, the last name of the file. */
    private fun total(conditional: String) = bindings(body(conditional)).last().also { assertThat(it).contains(":G_TOTAL=") }.substringAfter('=')

    private val decided = "Local(VARIABLE@6:2)"
    private val undecided = "Unknown[CONDITIONAL_COMPILATION]"

    @Test
    fun aLookupIsUndecidedOnlyByTextThatWritesTheName() {
        assertThat(total("$debug \$END")).isEqualTo(decided)
        // Dropped text that declares it, uses it, or tests it in the condition.
        assertThat(total("$debug \$ELSE g_total date; \$END")).isEqualTo(undecided)
        assertThat(total("$debug \$ELSE procedure dbg is begin g_total := 1; end; \$END")).isEqualTo(undecided)
        assertThat(total("\$IF pk_cfg.g_total > 0 \$THEN procedure dbg is begin null; end; \$END")).isEqualTo(undecided)
        // Kept text that declares it or uses it among the body's declarations.
        assertThat(total("\$IF \$\$debug \$THEN g_total date; \$END")).isEqualTo(undecided)
        assertThat(total("\$IF \$\$debug \$THEN g_copy number := g_total; \$END")).isEqualTo(undecided)
    }

    @Test
    fun aMentionIsAWordReadAsTheName() {
        // A quoted identifier is compared exactly: "g_total" is another name, "G_TOTAL" the same.
        assertThat(total("$debug \$ELSE \"g_total\" date; \$END")).isEqualTo(decided)
        assertThat(total("$debug \$ELSE \"G_TOTAL\" date; \$END")).isEqualTo(undecided)
        assertThat(total("$debug \$ELSE G_Total date; \$END")).isEqualTo(undecided)
        // Strings, q-quoted strings (an apostrophe inside), comments and inquiry directives are not names.
        assertThat(total("$debug \$ELSE procedure dbg is begin dbms_output.put_line('g_total'); end; \$END")).isEqualTo(decided)
        assertThat(total("$debug \$ELSE procedure dbg is begin dbms_output.put_line(q'[it's g_total]'); end; \$END")).isEqualTo(decided)
        assertThat(total("$debug \$ELSE procedure dbg is begin null; end; -- g_total\n  \$END")).isEqualTo(decided)
        assertThat(total("\$IF \$\$g_total \$THEN procedure dbg is begin null; end; \$END")).isEqualTo(decided)
    }

    @Test
    fun textThatMayChangeTheBlockStructureCountsForEveryName() {
        // A dropped branch that ends the block it is in and declares in the enclosing one.
        assertThat(total("$debug \$ELSE null; end; function f return number is begin return 1; \$END")).isEqualTo(undecided)
        // A kept branch that opens a block it does not end, starting among the body's declarations.
        assertThat(total("\$IF \$\$debug \$THEN g_dbg number; procedure dbg is begin null; \$ELSE procedure dbg is begin null; \$END end;"))
            .isEqualTo(undecided)
    }

    @Test
    fun aDroppedDeclarationInABlockAroundTheUseCounts() {
        val text = "create or replace procedure p2 is\n  g_total number;\n  n number;\nbegin\n  declare\n" +
            "    \$IF \$\$debug \$THEN x number; \$ELSE g_total date; \$END\n  begin\n    n := g_total;\n  end;\nend;\n/\n"
        assertThat(bindings(text)).contains("8:G_TOTAL=$undecided")
        assertThat(bindings(text.replace("\$ELSE g_total date; ", ""))).contains("8:G_TOTAL=Local(VARIABLE@2:2)")
    }

    @Test
    fun aUseInsideAKeptBranchStaysUndecided() {
        val text = "create or replace procedure p2 is\n  g_total number;\n  n number;\nbegin\n" +
            "  \$IF \$\$debug \$THEN n := g_total; \$END\n  n := g_total;\nend;\n/\n"
        assertThat(bindings(text).filter { "G_TOTAL" in it }).containsExactly("5:G_TOTAL=$undecided", "6:G_TOTAL=Local(VARIABLE@2:2)")
    }

    @Test
    fun aDeclarationIsUnusedWhenNoConditionalTextWritesIt() {
        val procedure = "create or replace procedure p(p_text varchar2) is\n  v_count number;\nbegin\n  CONDITIONAL\nend;\n/\n"
        fun variable(conditional: String) = usages(procedure.replace("CONDITIONAL", conditional), PlSqlGrammar.VARIABLE_DECLARATION)
        assertThat(variable("\$IF \$\$debug \$THEN dbms_output.put_line('x'); \$END")).containsExactly("V_COUNT: Unused")
        assertThat(variable("\$IF \$\$debug \$THEN null; \$ELSE v_count := 1; \$END")).containsExactly("V_COUNT: Unknown[CONDITIONAL_COMPILATION]")
        // A logging stub: the parameter is used only where the flag is off.
        assertThat(usages(procedure.replace("CONDITIONAL", "\$IF \$\$no_op \$THEN null; \$ELSE dbms_output.put_line(p_text); \$END"), PlSqlGrammar.PARAMETER_DECLARATION))
            .containsExactly("P_TEXT: Unknown[CONDITIONAL_COMPILATION]")
        // Declared only in a kept branch and written nowhere else: unused where it exists.
        val kept = "create or replace procedure p is\n  \$IF \$\$debug \$THEN v_dbg number; \$END\nbegin\n  null;\nend;\n/\n"
        assertThat(usages(kept, PlSqlGrammar.VARIABLE_DECLARATION)).containsExactly("V_DBG: Unused")
    }

    @Test
    fun cursorsAndTheirParametersAlike() {
        // The cursor is visible in the procedure's statements, its parameter in its query.
        val text = "create or replace procedure p is\n  cursor c_orders(p_id number) is select 1 a from dual QUERY;\nbegin\n  STATEMENTS\nend;\n/\n"
        fun evidence(query: String, statements: String) = usages(
            text.replace("QUERY", query).replace("STATEMENTS", statements), PlSqlGrammar.CURSOR_DECLARATION, PlSqlGrammar.CURSOR_PARAMETER_DECLARATION
        )
        assertThat(evidence("\$IF \$\$debug \$THEN where 1 = 1 \$END", "\$IF \$\$debug \$THEN null; \$END"))
            .containsExactly("C_ORDERS: Unused", "P_ID: Unused")
        assertThat(evidence("\$IF \$\$debug \$THEN \$ELSE where p_id = 1 \$END", "\$IF \$\$debug \$THEN null; \$ELSE open c_orders(1); \$END"))
            .containsExactly("C_ORDERS: Unknown[CONDITIONAL_COMPILATION]", "P_ID: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun anInnerDeclarationHidesOnlyWhatTheOuterTextMayDeclare() {
        val text = "declare\n  CONDITIONAL\n  x number;\nbegin\n  declare\n    v number;\n  begin\n    null;\n  end;\nend;\n/\n"
        fun inner(conditional: String) = declarations(text.replace("CONDITIONAL", conditional), PlSqlGrammar.VARIABLE_DECLARATION) { hidingEvidence(it) }
            .filter { it.startsWith("V:") }
        assertThat(inner("\$IF \$\$a \$THEN y number; \$END")).containsExactly("V: HidesNothing")
        assertThat(inner("\$IF \$\$a \$THEN y number; \$ELSE v number; \$END")).containsExactly("V: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun anExceptionsCodeIsUndecidedOnlyByTextThatWritesIt() {
        val text = "declare\n  e_late exception;\n  CONDITIONAL\nbegin\n  raise e_late;\nend;\n/\n"
        fun code(conditional: String): Any {
            var result: Any? = null
            walk(text.replace("CONDITIONAL", conditional), object : PlSqlCheck() {
                override fun leaveFile(node: AstNode) {
                    val binding = context.exceptionBinding(node.getFirstDescendant(PlSqlGrammar.RAISE_STATEMENT)!!.getFirstChild(PlSqlGrammar.VARIABLE_NAME))
                    result = (binding as? ExceptionBinding.Declared)?.exception?.errorCode ?: binding
                }
            })
            return result!!
        }
        assertThat(code("\$IF \$\$x \$THEN e_other exception; \$END")).isEqualTo(ExceptionErrorCode.None)
        assertThat(code("\$IF \$\$x \$THEN e_other exception; \$ELSE pragma exception_init(e_late, -20001); \$END").toString())
            .isEqualTo(ExceptionErrorCode.Unknown(setOf(UsageUncertainty.CONDITIONAL_COMPILATION)).toString())
    }

    /** `line:column:NAME=binding` of each segment of the qualified names of [text]. */
    private fun chains(text: String): List<String> {
        val rows = ArrayList<String>()
        walk(text, object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).flatMap { it.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME) }
                    .forEach { rows += "${it.token.line}:${it.token.column}:${it.tokenValue}=${context.nameBinding(it)}" }
            }
        })
        return rows.filterNot { it.contains(":DBMS_OUTPUT=") || it.contains(":PUT_LINE=") }
    }

    @Test
    fun aNameAfterARangeIsAMention() {
        val text = "create or replace procedure p_rng is\n  l_cnt number;\nbegin\n" +
            "  \$IF \$\$fast \$THEN null; \$ELSE for i in 1..l_cnt loop null; end loop; \$END\n  null;\nend;\n/\n"
        assertThat(usages(text, PlSqlGrammar.VARIABLE_DECLARATION)).containsExactly("L_CNT: Unknown[CONDITIONAL_COMPILATION]")
    }

    private val labeled = "create or replace procedure p_lbl is\n  v number := 1;\nbegin\n  CONDITIONAL\n  declare\n    v number := 2;\n" +
        "  begin\n    dbms_output.put_line(outer.v);\n  end;\nend;\n/\n"

    @Test
    fun aLabelInConditionalTextIsUndecidedFromTheBlockItMayLabel() {
        assertThat(chains(labeled.replace("CONDITIONAL", "\$IF \$\$lbl \$THEN <<outer>> \$END")))
            .containsExactly("8:25:OUTER=$undecided", "8:31:V=$undecided")
        val loop = "create or replace procedure p_loop is\nbegin\n  \$IF \$\$lbl \$THEN <<lp>> \$END\n" +
            "  for i in 1..3 loop\n    dbms_output.put_line(lp.i);\n  end loop;\nend;\n/\n"
        assertThat(chains(loop)).containsExactly("5:25:LP=$undecided", "5:28:I=$undecided")
        // Another label in the kept branch: as without the conditional text.
        val plain = chains(labeled.replace("CONDITIONAL", "null;"))
        assertThat(chains(labeled.replace("CONDITIONAL", "\$IF \$\$lbl \$THEN <<other>> \$END"))).isEqualTo(plain)
    }

    @Test
    fun aQualifiedUseThroughAnUndecidedQualifierMayUseTheMember() {
        fun inner(conditional: String) = usages(labeled.replace("CONDITIONAL", conditional), PlSqlGrammar.VARIABLE_DECLARATION)[1]
        assertThat(inner("\$IF \$\$lbl \$THEN null; \$ELSE <<outer>> \$END")).isEqualTo("V: Unknown[CONDITIONAL_COMPILATION]")
        assertThat(inner("\$IF \$\$lbl \$THEN null; \$ELSE <<other>> \$END")).isEqualTo("V: Unused")
    }

    @Test
    fun theAttributePositionIsUndecidedByHiddenTextInTheAttributeList() {
        val text = "create or replace type t_shape as object (\n  a number,\n  \$IF \$\$ext \$THEN b number, \$END\n  c number,\n" +
            "  member function get_c return number\n);\n/\ncreate or replace type body t_shape as\n" +
            "  member function get_c return number is\n  begin\n    return self.c + a;\n  end;\nend;\n/\n"
        fun c(source: String): String {
            var result = ""
            walk(source, object : PlSqlCheck() {
                override fun leaveFile(node: AstNode) {
                    val member = node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).single()
                    result = context.fieldBinding(member.lastChild).toString()
                }
            })
            return result
        }
        assertThat(c(text)).isEqualTo(undecided)
        assertThat(c(text.replace("  \$IF \$\$ext \$THEN b number, \$END\n", "\n"))).startsWith("Field(ATTRIBUTE#1")
    }

    private val keptProcedures = "create or replace package body pk_kept is\n  \$if \$\$v21 \$then\n" +
        "  procedure one is\n    l_msg varchar2(10);\n    l_unused varchar2(10);\n  begin\n    l_msg := 'a';\n  end;\n" +
        "  procedure two is\n    l_msg varchar2(10);\n    l_unused varchar2(10);\n  begin\n    l_msg := 'b';\n    l_unused := 'c';\n  end;\n" +
        "  \$end\n  procedure three is begin null; end;\nend;\n/\n"

    @Test
    fun aKeptBranchHidesAUseOnlyByADeclarationThatShadows() {
        // The same names declared and used in a sibling procedure of the same kept branch shadow nothing. The uses
        // written in the kept branch are possible uses (the branch may not be compiled), not proven ones.
        val cc = "Unknown[CONDITIONAL_COMPILATION]"
        assertThat(usages(keptProcedures, PlSqlGrammar.VARIABLE_DECLARATION))
            .containsExactly("L_MSG: $cc", "L_UNUSED: Unused", "L_MSG: $cc", "L_UNUSED: $cc")
    }

    @Test
    fun aKeptDeclarationThatShadowsMayHideAUse() {
        // Nested: where `${'$'}${'$'}a` is not set, `v := 1` uses the outer `v`.
        val nested = "create or replace procedure p_shadow is\n  v number;\nbegin\n  declare\n    \$IF \$\$a \$THEN v number; \$END\n" +
            "  begin\n    v := 1;\n  end;\nend;\n/\n"
        assertThat(usages(nested, PlSqlGrammar.VARIABLE_DECLARATION).first()).isEqualTo("V: Unknown[CONDITIONAL_COMPILATION]")
        // The same region: a duplicate in a kept branch.
        val duplicate = "create or replace procedure p_dup is\n  v number;\n  \$IF \$\$a \$THEN v number; \$END\nbegin\n  v := 1;\nend;\n/\n"
        assertThat(usages(duplicate, PlSqlGrammar.VARIABLE_DECLARATION).first()).isNotEqualTo("V: Unused")
    }

    @Test
    fun droppedTextAndLabelsStillCount() {
        val dropped = "create or replace procedure p_drop is\n  \$IF \$\$a \$THEN v_kept number; \$ELSE v_kept number := 1; x number := v_kept; \$END\n" +
            "begin\n  null;\nend;\n/\n"
        assertThat(usages(dropped, PlSqlGrammar.VARIABLE_DECLARATION)).containsExactly("V_KEPT: Unknown[CONDITIONAL_COMPILATION]")
        val label = "create or replace procedure p_label is\n  d_name number;\nbegin\n  \$IF \$\$a \$THEN <<d_name>> \$END\n  null;\nend;\n/\n"
        assertThat(usages(label, PlSqlGrammar.VARIABLE_DECLARATION)).containsExactly("D_NAME: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun parametersAndCursorsAlike() {
        val sibling = "create or replace package body pk_kept is\n  \$if \$\$v21 \$then\n" +
            "  procedure one(p_code varchar2) is\n    cursor c_rows is select 1 a from dual;\n  begin\n    null;\n  end;\n" +
            "  procedure two(p_code varchar2) is\n    cursor c_rows is select 1 a from dual;\n  begin\n    open c_rows;\n    dbms_output.put_line(p_code);\n  end;\n" +
            "  \$end\nend;\n/\n"
        assertThat(usages(sibling, PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_DECLARATION))
            .containsExactly("P_CODE: Unused", "C_ROWS: Unused", "P_CODE: Unknown[CONDITIONAL_COMPILATION]", "C_ROWS: Unknown[CONDITIONAL_COMPILATION]")
        // Redeclared inside the procedure by the kept branch: where it is not compiled, the uses may bind the outer ones.
        val shadowing = "create or replace procedure p_one(p_code varchar2) is\n  cursor c_rows is select 1 a from dual;\nbegin\n" +
            "  declare\n    \$IF \$\$a \$THEN p_code varchar2(1); cursor c_rows is select 2 b from dual; \$END\n" +
            "  begin\n    open c_rows;\n    dbms_output.put_line(p_code);\n  end;\nend;\n/\n"
        assertThat(usages(shadowing, PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_DECLARATION).take(2))
            .containsExactly("P_CODE: Unknown[CONDITIONAL_COMPILATION]", "C_ROWS: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun aKeptAliasMayChangeWhatAReferenceOutsideTheBranchBinds() {
        // Where `${'$'}${'$'}al` is set, `a` is the alias of `t1` and `a.x` reads its column; where it is not, the record.
        val alias = "create or replace procedure p_alias is\n  type t_r is record (x number);\n  a t_r;\n  y number;\nbegin\n" +
            "  select a.x into y from t1 \$IF \$\$al \$THEN a \$END where rownum = 1;\nend;\n/\n"
        assertThat(usages(alias, PlSqlGrammar.VARIABLE_DECLARATION).first()).isEqualTo("A: Unknown[CONDITIONAL_COMPILATION]")
        // A kept column alias that the outer query reads, with a local of the same name.
        val column = "create or replace procedure p_col is\n  total number;\n  y number;\nbegin\n" +
            "  select total into y from (select 1 \$IF \$\$al \$THEN total \$END from dual);\nend;\n/\n"
        assertThat(usages(column, PlSqlGrammar.VARIABLE_DECLARATION).first()).isNotEqualTo("TOTAL: Unused")
    }

    private fun aliased(conditional: String) = "create or replace procedure p_alias is\n  type t_r is record (x number);\n  a t_r;\n  y number;\n" +
        "begin\n  select a.x into y from t1 $conditional where rownum = 1;\nend;\n/\n"

    @Test
    fun anAliasWrittenInConditionalTextLeavesTheQualifiedNameUndecided() {
        val both = listOf("6:9:A=$undecided", "6:11:X=$undecided")
        assertThat(chains(aliased("\$IF \$\$al \$THEN a \$END"))).isEqualTo(both)
        assertThat(chains(aliased("\$IF \$\$al \$THEN \$ELSE a \$END"))).isEqualTo(both)
        // Another alias, or text that writes no alias: as without the conditional text.
        val plain = chains(aliased(""))
        assertThat(plain).noneMatch { it.contains("CONDITIONAL") }
        assertThat(chains(aliased("\$IF \$\$al \$THEN b \$END"))).isEqualTo(plain)
        assertThat(chains(aliased("\$IF \$\$al \$THEN \$ELSE b \$END"))).isEqualTo(plain)
        assertThat(chains(aliased("where 1 = 1 \$IF \$\$x \$THEN and c = 1 \$END and").replace("where rownum", "rownum")))
            .isEqualTo(plain)
    }

    @Test
    fun aWithNameWrittenInConditionalTextLeavesTheQualifiedNameUndecided() {
        fun with(clause: String) = "create or replace procedure p_with is\n  type t_r is record (x number);\n  w t_r;\n  y number;\n" +
            "begin\n  $clause select w.x into y from t1;\nend;\n/\n"
        for (clause in listOf("\$IF \$\$w \$THEN with w as (select 1 x from dual) \$END", "\$IF \$\$w \$THEN \$ELSE with w as (select 1 x from dual) \$END")) {
            val rows = chains(with(clause))
            assertThat(rows.map { it.substringAfter(":").substringAfter(":") }).containsExactly("W=$undecided", "X=$undecided")
        }
    }

    @Test
    fun droppedTextAtTheEndOfAStatementOrInlineViewIsPartOfIt() {
        fun body(statement: String) = "create or replace procedure p_end is\n  type t_r is record (x number);\n  a t_r;\n  y number;\n" +
            "begin\n  $statement\nend;\n/\n"
        fun a(statement: String) = chains(body(statement)).map { it.substringAfter(":").substringAfter(":") }.dropWhile { !it.startsWith("A=") }
        val both = listOf("A=$undecided", "X=$undecided")
        assertThat(a("select a.x into y from t1 \$IF \$\$al \$THEN \$ELSE a \$END;")).isEqualTo(both)
        assertThat(a("select a.x into y from t1 \$IF \$\$al \$THEN b \$ELSE a \$END;")).isEqualTo(both)
        assertThat(a("select v.x into y from (select a.x from t1 \$IF \$\$al \$THEN \$ELSE a \$END) v;")).isEqualTo(both)
        // After the statement's end: as without the conditional text.
        val after = chains(body("select a.x into y from t1; \$IF \$\$al \$THEN \$ELSE y := a.x; \$END"))
        assertThat(after.first()).isEqualTo(chains(body("select a.x into y from t1;")).first())
        assertThat(after.first()).contains("A=Local(VARIABLE")
    }
}
