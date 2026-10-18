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
package com.felipebz.zpa.api.dynamicsql

import com.felipebz.flr.api.AstNode
import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.api.Token
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.SequenceReferenceResolution
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** The SQL text built by dynamic statements, asked for by a check during a scan. */
@OptIn(ZpaExperimentalApi::class)
class DynamicSqlTextTest {
    private val fileId = FileId("dyn.sql")

    private class FixtureFile(private val id: FileId, private val source: String) : PlSqlFile {
        override fun contents() = source
        override fun fileName() = id.value
        override fun path(): Path = Path.of(id.value)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** Asks for the text of every `EXECUTE IMMEDIATE` and `OPEN … FOR`, twice (the second must be the cached result). */
    private class Probe(private val inspect: (Probe, DynamicSqlText) -> Unit = { _, _ -> }) : PlSqlCheck() {
        fun analysis() = projectAnalysis()
        val results = mutableListOf<DynamicSqlText?>()
        val expressions = mutableListOf<AstNode>()

        override fun init() {
            subscribeTo(PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT, PlSqlGrammar.OPEN_FOR_STATEMENT)
        }

        override fun visitNode(node: AstNode) {
            val expression = if (node.type === PlSqlGrammar.EXECUTE_IMMEDIATE_STATEMENT) node.children[2] else node.children[3]
            val text = context.dynamicSql(expression)
            assertThat(context.dynamicSql(expression)).isSameAs(text)
            expressions += expression
            results += text
            text?.let { inspect(this, it) }
        }
    }

    private fun scan(source: String, context: ProjectAnalysisContext = ProjectAnalysisContext.NOT_PREPARED, probe: Probe = Probe()): Probe {
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context).scanFile(FixtureFile(fileId, source), listOf(probe), fileId)
        return probe
    }

    /** The text of the single dynamic statement of a block running [expression]. */
    private fun text(expression: String): DynamicSqlText? = scan("begin\n  execute immediate $expression;\nend;\n").results.single()

    private fun built(expression: String): String? = text(expression)?.text

    private fun position(source: String, needle: String, occurrence: Int = 0): Pair<Int, Int> {
        var at = -1
        repeat(occurrence + 1) { at = source.indexOf(needle, at + 1) }
        require(at >= 0) { needle }
        val before = source.substring(0, at)
        return (before.count { it == '\n' } + 1) to (at - (before.lastIndexOf('\n') + 1))
    }

    private fun tokens(text: DynamicSqlText): List<Token> = text.root.tokens.filter { it.type !== GenericTokenType.EOF }
    private fun token(text: DynamicSqlText, value: String, occurrence: Int = 0) = tokens(text).filter { it.originalValue == value }[occurrence]
    private fun at(token: Token) = token.line to token.column

    // Text.

    @Test
    fun literalsDecodeToTheValueOracleSees() {
        assertThat(built("'select ''x'' from dual'")).isEqualTo("select 'x' from dual")
        assertThat(built("N'select 1 from dual'")).isEqualTo("select 1 from dual")
        assertThat(built("q'[select 'a' from dual]'")).isEqualTo("select 'a' from dual")
        assertThat(built("q'{select 1 from dual}'")).isEqualTo("select 1 from dual")
        assertThat(built("q'(select 1 from dual)'")).isEqualTo("select 1 from dual")
        assertThat(built("q'<select 1 from dual>'")).isEqualTo("select 1 from dual")
        assertThat(built("q'#select 'b' from dual#'")).isEqualTo("select 'b' from dual")
        assertThat(built("nq'[select 1 from dual]'")).isEqualTo("select 1 from dual")
        assertThat(built("Q'!select 1 from dual!'")).isEqualTo("select 1 from dual")
    }

    @Test
    fun chrAndParenthesizedConcatenationsJoinIntoTheText() {
        assertThat(built("'select 1' || chr(10) || 'from dual'")).isEqualTo("select 1\nfrom dual")
        assertThat(built("('select ' || '1') || ' from dual'")).isEqualTo("select 1 from dual")
        val text = text("('select ' || x) || ' from dual'")!!
        assertThat(text.text).isEqualTo("select ZPA$1 from dual")
        assertThat(text.parts.map { it.type }).containsExactly(PlSqlGrammar.LITERAL, PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.LITERAL)
        assertThat(text.expression!!.type).isEqualTo(PlSqlGrammar.CONCATENATION_EXPRESSION)
    }

    // Holes.

    @Test
    fun holesStandForValuesNamesAndConditions() {
        val value = text("'select id from company where id = ' || p_id")!!
        assertThat(value.text).isEqualTo("select id from company where id = ZPA$1")
        assertThat(value.holesDropped).isFalse()
        assertThat(value.holes.map { it.placeholder to it.expression.tokenOriginalValue }).containsExactly("ZPA$1" to "p_id")
        assertThat(built("'delete from ' || p_table")).isEqualTo("delete from ZPA$1")
        assertThat(built("'select * from company where ' || v_cond")).isEqualTo("select * from company where ZPA$1")
        assertThat(built("'select ' || p_col || '_x from t'")).isEqualTo("select ZPA$1_x from t")
        assertThat(built("'select id from tab_' || v || ' t'")).isEqualTo("select id from tab_ZPA$1 t")
        // Inside a string of the built SQL the hole is just text of that string.
        assertThat(built("'select 1 from t where c = ''' || v || ''''")).isEqualTo("select 1 from t where c = 'ZPA$1'")
        // Two holes side by side stay two names.
        assertThat(built("'insert into ' || a || b || ' values (1)'")).isEqualTo("insert into ZPA$1 ZPA$2 values (1)")
    }

    @Test
    fun aClauseFragmentParsesOnceTheHolesAreDropped() {
        val text = text("'select id from t where id = 1 ' || v_more")!!
        assertThat(text.holesDropped).isTrue()
        assertThat(text.text).isEqualTo("select id from t where id = 1 ")
        assertThat(text.holes.single().expression.tokenOriginalValue).isEqualTo("v_more")
        assertThat(text.root.tokens.none { it.originalValue.startsWith("ZPA$") }).isTrue()
    }

    @Test
    fun placeholdersAreRecognizedAndPointAtTheirHoles() {
        val text = text("'insert into ' || p_table || ' (a) values (' || p_value || ')'")!!
        val names = text.root.descendants().filter { text.isPlaceholder(it) && !it.hasChildren() }
        assertThat(names.map { text.holeOf(it)!!.placeholder }).containsExactly("ZPA$1", "ZPA$2")
        assertThat(names.map { text.holeOf(it)!!.expression.tokenOriginalValue }).containsExactly("p_table", "p_value")
        assertThat(text.root.descendants().filter { it.type === PlSqlGrammar.IDENTIFIER_NAME && it.tokenValue == "a" }.none(text::isPlaceholder)).isTrue()
        assertThat(text.holesIn(text.root).map { it.placeholder }).containsExactly("ZPA$1", "ZPA$2")
        // A glued name is not a placeholder, but holds one.
        val glued = text("'select id from tab_' || v")!!
        val name = token(glued, "tab_ZPA$1")
        val node = glued.root.descendants().first { !it.hasChildren() && it.token === name }
        assertThat(glued.isPlaceholder(node)).isFalse()
        assertThat(glued.holesIn(node).single().placeholder).isEqualTo("ZPA$1")
    }

    @Test
    fun thePlaceholderNeverOccursInTheKnownText() {
        val text = text("'select zpa\$1 from t where id = ' || v")!!
        assertThat(text.holes.single().placeholder).isEqualTo("ZPA$$1")
        assertThat(text.root.descendants().count { text.isPlaceholder(it) && !it.hasChildren() }).isEqualTo(1)
    }

    // Positions.

    @Test
    fun tokensCarryTheirPositionInTheFile() {
        val source = "begin\n  execute immediate 'select a, ''x'', ''y'', b from t where id = ' || p_id || ' and c = 1';\nend;\n"
        val text = scan(source).results.single()!!
        assertThat(at(token(text, "select"))).isEqualTo(position(source, "select"))
        // Past two escapes on the same line.
        assertThat(at(token(text, "b"))).isEqualTo(position(source, "b from"))
        assertThat(at(token(text, "ZPA$1"))).isEqualTo(position(source, "p_id"))
        // The second literal of the concatenation.
        assertThat(at(token(text, "and"))).isEqualTo(position(source, "and c"))
        assertThat(at(token(text, "c"))).isEqualTo(position(source, "c = 1"))
        // A nested literal keeps the built value: its end falls short by its escapes.
        assertThat(token(text, "'x'").column).isEqualTo(position(source, "''x''").second)
    }

    @Test
    fun linesOfAMultilineLiteralKeepTheirColumnsAndCommentsMove() {
        val source = "begin\n  execute immediate 'select a,\n      b -- note\n    from t';\nend;\n"
        val text = scan(source).results.single()!!
        assertThat(at(token(text, "select"))).isEqualTo(position(source, "select"))
        assertThat(at(token(text, "b"))).isEqualTo(3 to 6)
        assertThat(at(token(text, "from"))).isEqualTo(4 to 4)
        val comment = token(text, "from").trivia.single { it.isComment }.token
        assertThat(comment.originalValue).isEqualTo("-- note")
        assertThat(at(comment)).isEqualTo(position(source, "-- note"))
    }

    @Test
    fun alternativeQuotingStartsAfterItsDelimiter() {
        val source = "begin\n  execute immediate q'[select 'a' from dual]';\nend;\n"
        val text = scan(source).results.single()!!
        assertThat(at(token(text, "select"))).isEqualTo(position(source, "select"))
        assertThat(at(token(text, "dual"))).isEqualTo(position(source, "dual"))
    }

    // Parse.

    @Test
    fun eachStatementKindParsesAsOneStatement() {
        listOf(
            "'update company set st_active = 1 where id = 2'" to com.felipebz.zpa.api.DmlGrammar.DML_COMMAND,
            "'with p as (select 1 id from dual) select id from p'" to com.felipebz.zpa.api.DmlGrammar.DML_COMMAND,
            "'begin pack_process.write_log(''x''); end;'" to PlSqlGrammar.COMPILATION_UNIT,
            "'truncate table company'" to com.felipebz.zpa.api.DdlGrammar.DDL_COMMAND,
            "'drop package pack_process'" to com.felipebz.zpa.api.DdlGrammar.DDL_COMMAND,
            "'LOCK TABLE company IN EXCLUSIVE MODE WAIT 60'" to null,
            "'ALTER SESSION SET NLS_NUMERIC_CHARACTERS = '',.'''" to null
        ).forEach { (expression, kind) ->
            val text = text(expression)
            assertThat(text).describedAs(expression).isNotNull
            val statements = text!!.root.children.filter { it.type !== GenericTokenType.EOF }
            assertThat(statements).describedAs(expression).hasSize(1)
            if (kind != null) assertThat(statements.single().type).describedAs(expression).isEqualTo(kind)
            assertThat(text.holesDropped).isFalse()
        }
    }

    @Test
    fun textsThatAreNotOneStatementGiveNothing() {
        assertThat(text("'select 1 from dual; select 2 from dual'")).isNull()
        assertThat(text("'update company set ' || v_columns || ' where'")).isNull()
        assertThat(text("'   '")).isNull()
        assertThat(text("v_sql")).isNull()
        assertThat(text("build_sql(1)")).isNull()
        assertThat(text("1")).isNull()
        assertThat(text("chr(10)")).isNull()
    }

    @Test
    fun theTextOfOpenForIsParsedToo() {
        val probe = scan("declare c sys_refcursor; begin open c for 'select id from company where id = ' || 1; end;\n")
        assertThat(probe.results.single()!!.text).isEqualTo("select id from company where id = ZPA$1")
    }

    // Symbols.

    @Test
    fun namesInTheTextDoNotSeeTheRunningCode() {
        val probe = scan("create procedure p(x number) is begin execute immediate 'begin dbms_output.put_line(x); end;'; end;\n/\n")
        val text = probe.results.single()!!
        val x = text.root.descendants().filter { it.tokenOrNull?.originalValue == "x" }
        assertThat(x).isNotEmpty
        assertThat(x.map { (it as SemanticAstNode).symbol }).containsOnlyNulls()
        assertThat(text.symbolTable.symbols.map { it.name }).doesNotContain("x")
    }

    @Test
    fun declarationsInTheTextBindInsideIt() {
        val text = text("'declare v number; begin v := 1; end;'")!!
        val v = text.root.descendants().filter { it.tokenOrNull?.originalValue == "v" }.mapNotNull { (it as SemanticAstNode).symbol }.distinct()
        assertThat(v).hasSize(1)
        assertThat(v.single().declaration.tokens.first().line).isEqualTo(2)
        assertThat(v.single().usages).isNotEmpty
        assertThat(text.symbolTable.symbols).contains(v.single())
    }

    // Project.

    @Test
    fun aSequenceReadInTheTextResolvesAsInStaticCode() {
        val sequenceFile = FileId("seq.sql")
        val source = "begin\n  execute immediate 'select seq.nextval from dual';\n  select seq.nextval into n from dual;\nend;\n"
        val prepared = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(
                listOf(ProjectSource(sequenceFile) { "create sequence seq;" }, ProjectSource(fileId) { source }),
                concurrent = false
            )
        )
        val resolutions = mutableListOf<SequenceReferenceResolution>()
        val probe = Probe { check, text ->
            text.root.descendants().filter { it.type === PlSqlGrammar.MEMBER_EXPRESSION }.forEach {
                resolutions += check.analysis().resolveSequenceReference(it)
            }
        }
        val static = object : PlSqlCheck() {
            val results = mutableListOf<SequenceReferenceResolution>()
            override fun init() = subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION)
            override fun visitNode(node: AstNode) {
                results += projectAnalysis().resolveSequenceReference(node)
            }
        }
        AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, prepared).scanFile(FixtureFile(fileId, source), listOf(probe, static), fileId)
        assertThat(resolutions).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
        assertThat(static.results).containsExactly(SequenceReferenceResolution.RESOLVED_SEQUENCE)
    }

    @Test
    fun aDeclarationInTheTextIsNotAProjectDeclaration() {
        val source = "begin\n  execute immediate 'create sequence made_at_run_time';\n  execute immediate 'select made_at_run_time.nextval from dual';\nend;\n"
        val prepared = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare(listOf(ProjectSource(fileId) { source }), concurrent = false)
        )
        assertThat(prepared.state).isInstanceOf(ProjectAnalysisContext.State.Prepared::class.java)
        val index = (prepared.state as ProjectAnalysisContext.State.Prepared).result.index
        assertThat(index.findSequences(com.felipebz.zpa.project.QualifiedName(listOf(com.felipebz.zpa.project.OracleIdentifier.fromSource("made_at_run_time"))))).isEmpty()
        val resolutions = mutableListOf<SequenceReferenceResolution>()
        val probe = Probe { check, text ->
            text.root.descendants().filter { it.type === PlSqlGrammar.MEMBER_EXPRESSION }.forEach {
                resolutions += check.analysis().resolveSequenceReference(it)
            }
        }
        scan(source, prepared, probe)
        assertThat(probe.results).doesNotContainNull()
        assertThat(resolutions).containsExactly(SequenceReferenceResolution.UNKNOWN)
    }

    @Test
    fun theListOfPartsEntryPointJoinsPartsFromAnywhere() {
        val probe = object : PlSqlCheck() {
            var text: DynamicSqlText? = null
            override fun init() = subscribeTo(PlSqlGrammar.ANONYMOUS_BLOCK)
            override fun visitNode(node: AstNode) {
                val literals = node.getDescendants(PlSqlGrammar.LITERAL).filter { it.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL) }
                val variable = node.getDescendants(PlSqlGrammar.VARIABLE_NAME).first { it.tokenOriginalValue == "p_id" }
                val parts = listOf(literals[0], variable, literals[1])
                text = context.dynamicSql(parts)
                assertThat(context.dynamicSql(parts.toList())).isSameAs(text)
            }
        }
        val source = "declare\n  a varchar2(100) := 'select id from company where id = ';\n  b varchar2(10) := ' and 1 = 1';\nbegin\n  run(a, p_id, b);\nend;\n"
        AstScanner(emptyList(), null, true).scanFile(FixtureFile(fileId, source), listOf(probe), fileId)
        assertThat(probe.text!!.text).isEqualTo("select id from company where id = ZPA$1 and 1 = 1")
        assertThat(probe.text!!.expression).isNull()
        assertThat(at(token(probe.text!!, "and"))).isEqualTo(position(source, "and 1"))
    }

    private fun AstNode.descendants(): List<AstNode> = children.flatMap { listOf(it) + it.descendants() }
}
