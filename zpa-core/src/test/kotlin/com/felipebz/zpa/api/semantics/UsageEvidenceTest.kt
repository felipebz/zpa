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
@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class UsageEvidenceTest {

    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    /** `NAME: evidence` for each variable and exception declaration of [text], analyzed with [others] as the project. */
    private fun evidence(text: String, vararg others: Pair<String, String>, prepared: Boolean = true): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    /** `NAME: evidence` for each parameter and cursor parameter declaration of [text], with no project. */
    private fun parameters(text: String): List<String> {
        val project = ProjectAnalysisContext.NOT_PREPARED
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION, PlSqlGrammar.CURSOR_PARAMETER_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    private fun cursors(text: String): List<String> {
        val project = ProjectAnalysisContext.NOT_PREPARED
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.CURSOR_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.usageEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    @Test
    fun usedUnusedAndUnknownWithTheirReasons() {
        val text = "declare\n  used number;\n  unused number;\n  in_sql number;\n  e exception;\n  n number;\nbegin\n" +
            "  used := 1;\n  select count(in_sql) into n from some_table;\n  raise e;\nend;\n/\n"
        assertThat(evidence(text)).containsExactly(
            "USED: Used",
            "UNUSED: Unused",
            "IN_SQL: Unknown[SQL_NAME_MAY_BE_COLUMN]",
            "E: Used",
            "N: Used"
        )
    }

    @Test
    fun textTheAnalysisCannotSeeIsNeverAProvenAbsence() {
        val conditional = "declare\n  v number;\nbegin\n  \$if \$\$x \$then null; \$else v := 1; \$end\nend;\n/\n"
        assertThat(evidence(conditional)).containsExactly("V: Unknown[CONDITIONAL_COMPILATION]")
        val substitution = "declare\n  w number;\n  n number;\nbegin\n  n := 1 &plus_w;\nend;\n/\n"
        assertThat(evidence(substitution)).containsExactly("W: Unknown[SUBSTITUTION]", "N: Unknown[SUBSTITUTION]")
    }

    @Test
    fun aReferenceBeforeTheDeclarationIsAUse() {
        assertThat(evidence("declare\n  a number := b;\n  b number := 1;\nbegin\n  null;\nend;\n/\n")).containsExactly("A: Unused", "B: Used")
    }

    @Test
    fun parametersAndCursorParameters() {
        val text = "create procedure p(used number, unused number, named number) is\n" +
            "  cursor c(in_query number, unused_c number) is select count(*) from t where in_query = 1;\n" +
            "begin\n  print(p.used);\n  q(named => 1);\nend;\n/\n" +
            "create procedure ext(x number) as language java name 'A.run(int)';\n/\n" +
            "create package s as\n  procedure h(hp number);\nend;\n/\n"
        assertThat(parameters(text)).containsExactly(
            "USED: Used",
            "UNUSED: Unused",
            "NAMED: Unused",
            "IN_QUERY: Unknown[SQL_NAME_MAY_BE_COLUMN]",
            "UNUSED_C: Unused",
            "X: Unavailable(NOT_A_COVERED_DECLARATION)",
            "HP: Unavailable(NOT_A_COVERED_DECLARATION)"
        )
    }

    @Test
    fun cursors() {
        val text = "declare\n  cursor c_rowtype is select 1 a from dual;\n  r c_rowtype%rowtype;\n" +
            "  cursor c_attribute is select 1 from dual;\n  cursor dummy is select 1 from dual;\n" +
            "  cursor c_unused is select 1 from dual;\n  v varchar2(1);\nbegin\n" +
            "  if c_attribute%isopen then null; end if;\n  select dummy into v from dual;\nend;\n/\n"
        assertThat(cursors(text)).containsExactly(
            "C_ROWTYPE: Used",
            "C_ATTRIBUTE: Used",
            "DUMMY: Unknown[UNSUPPORTED_REFERENCE]",
            "C_UNUSED: Unused"
        )
    }

    /** `NAME: hiding evidence` for each variable and exception declaration of [text]. */
    private fun hiding(text: String, vararg others: Pair<String, String>, prepared: Boolean = true): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init {
                subscribeTo(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.EXCEPTION_DECLARATION)
            }

            override fun visitNode(node: AstNode) {
                rows += "${node.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME).tokenValue}: ${context.hidingEvidence(node)}"
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    @Test
    fun hidingFollowsOracleVisibility() {
        val text = "create procedure p(par number) is\n  v number;\nbegin\n  for i in 1..2 loop\n    declare\n" +
            "      v number;\n      par number;\n      i number;\n      fresh number;\n    begin\n      null;\n    end;\n" +
            "  end loop;\nend;\n/\ndeclare\n  d number;\n  d number;\nbegin\n  null;\nend;\n/\n"
        assertThat(hiding(text)).containsExactly(
            "V: HidesNothing",
            "V: Hides(2:2)",
            "PAR: Hides(1:19)",
            "I: Hides(4:6)",
            "FRESH: HidesNothing",
            "D: HidesNothing",
            "D: HidesNothing"
        )
    }

    @Test
    fun aPairTheAnalysisCannotFullySeeIsUnknown() {
        val text = "declare\n  y number;\nbegin\n  \$if \$\$a \$then null; \$end\n  declare\n    y number;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(hiding(text)).containsExactly("Y: HidesNothing", "Y: Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun aHiddenDeclarationNamesItsFileTheSameWayOnEveryMachine() {
        fun name(fileId: String?) = HiddenDeclaration(2, 2, fileId, null).fileName
        assertThat(name("counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("src/db/counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("file:///home/runner/work/my%20app/src/counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name("C:\\work\\app\\counter_pkg.pks")).isEqualTo("counter_pkg.pks")
        assertThat(name(null)).isNull()
    }

    @Test
    fun exceptionNamesBindTheirDeclarations() {
        val text = "declare\n  e exception;\n  no_data_found exception;\nbegin\n  begin\n    raise e;\n  exception\n    when e then raise no_data_found;\n" +
            "    when no_data_found then raise too_many_rows;\n    when too_many_rows then raise pkg.e;\n  end;\nend;\n/\n"
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val bindings = ArrayList<ExceptionBinding>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                val names = node.getDescendants(PlSqlGrammar.RAISE_STATEMENT, PlSqlGrammar.EXCEPTION_HANDLER).sortedBy { it.token.line * 1000 + it.token.column }
                    .mapNotNull { it.getFirstChildOrNull(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.MEMBER_EXPRESSION) }
                names.forEach { bindings += this.context.exceptionBinding(it) }
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        // raise e / when e, raise no_data_found / when no_data_found (the local one), raise too_many_rows, when too_many_rows, raise pkg.e
        assertThat(bindings.map { it.toString() }).containsExactly(
            "Declared(BLOCK@2:2)", "Declared(BLOCK@2:2)", "Declared(BLOCK@3:2)", "Declared(BLOCK@3:2)",
            "Predefined(TOO_MANY_ROWS)", "Predefined(TOO_MANY_ROWS)", "NotLocal"
        )
        assertThat(bindings[0]).isInstanceOf(ExceptionBinding.Declared::class.java)
        assertThat((bindings[0] as ExceptionBinding.Declared).exception).isEqualTo((bindings[1] as ExceptionBinding.Declared).exception)
        assertThat((bindings[0] as ExceptionBinding.Declared).exception).isNotEqualTo((bindings[2] as ExceptionBinding.Declared).exception)
    }

    @Test
    fun exceptionsTiedToAnErrorCode() {
        val text = "declare\n  e1 exception;\n  pragma exception_init(e1, -20001);\n  e2 exception;\nbegin\n  raise e1;\n  raise e2;\nend;\n/\n" +
            "declare\n  e3 exception;\n  \$if \$\$x \$then pragma exception_init(e3, 100); \$end\nbegin\n  raise e3;\nend;\n/\n"
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val codes = ArrayList<Any>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.RAISE_STATEMENT).forEach {
                    val binding = this.context.exceptionBinding(it.getFirstChild(PlSqlGrammar.VARIABLE_NAME))
                    codes += (binding as? ExceptionBinding.Declared)?.exception?.errorCode ?: binding
                }
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        // A raise in the region the conditional compilation hides is not bound at all.
        assertThat(codes).containsExactly(
            ExceptionErrorCode.Associated(-20001), ExceptionErrorCode.None,
            ExceptionBinding.Unknown(setOf(UsageUncertainty.CONDITIONAL_COMPILATION))
        )
    }

    @Test
    fun plainNamesBindLocalDeclarationsOrNothing() {
        val text = "declare\n  v date;\n  user varchar2(30);\nbegin\n  v := sysdate;\n  select sysdate into v from t;\n" +
            "  select user into v from t;\n  v := user;\nend;\n/\nbegin\n  x.y := 1;\nend;\n/\n"
        assertThat(nameBindings(text)).containsExactly(
            "5:V=Local(VARIABLE@2:2)", "5:SYSDATE=NothingLocal",
            "6:SYSDATE=NothingLocal(column possible)", "6:V=Local(VARIABLE@2:2)",
            "7:USER=ColumnOrLocal(VARIABLE@3:2)", "7:V=Local(VARIABLE@2:2)",
            "8:V=Local(VARIABLE@2:2)", "8:USER=Local(VARIABLE@3:2)",
            "12:X=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun overDualOnlyDummyIsAColumn() {
        val text = "declare\n  v number;\n  dummy varchar2(1);\nbegin\n  select v into v from dual;\n  select dummy into dummy from sys.dual;\n" +
            "  select w into v from dual;\n  with dual as (select 1 v from t) select v into v from dual;\nend;\n/\n"
        assertThat(nameBindings(text).filter { it.startsWith("5:") || it.startsWith("6:") || it.startsWith("7:W") || it.startsWith("8:") }).containsExactly(
            "5:V=Local(VARIABLE@2:2)", "5:V=Local(VARIABLE@2:2)",
            "6:DUMMY=ColumnOrLocal(VARIABLE@3:2)", "6:DUMMY=Local(VARIABLE@3:2)",
            "7:W=NothingLocal",
            "8:V=ColumnOrLocal(VARIABLE@2:2)", "8:V=Local(VARIABLE@2:2)"
        )
    }

    @Test
    fun sqlNamesInABodyWhoseSpecificationIsInAnotherFile() {
        val spec = "create package pkg is\n  procedure p;\nend;\n/\n"
        val declaringSpec = "create package pkg is\n  col number;\n  procedure p;\nend;\n/\n"
        val body = "create package body pkg is\n  procedure p is\n    n number;\n  begin\n    select col into n from tab, tab2;\n  end;\nend;\n/\n"
        val bodyCol = "create package body pkg is\n  col number;\n  procedure p is\n    n number;\n  begin\n    select col into n from tab, tab2;\n  end;\nend;\n/\n"
        // The project shows the specification does not declare it; without the project it may.
        assertThat(nameBindings(body, "s.sql" to spec, prepared = true)).contains("5:COL=NothingLocal(column possible)")
        assertThat(nameBindings(body, "s.sql" to spec)).contains("5:COL=Unknown[SQL_NAME_MAY_BE_COLUMN, DECLARATION_OUTSIDE_FILE]")
        // As with the specification in the file: a column, or the specification's variable.
        assertThat(nameBindings(body, "s.sql" to declaringSpec, prepared = true)).contains("5:COL=ColumnOrLocal(VARIABLE@s.sql:2:2)")
        assertThat(nameBindings(declaringSpec + body)).contains("10:COL=ColumnOrLocal(VARIABLE@2:2)")
        // A declaration of the body.
        assertThat(nameBindings(bodyCol, "s.sql" to spec, prepared = true)).contains("6:COL=ColumnOrLocal(VARIABLE@2:2)")
        assertThat(nameBindings(spec + bodyCol)).contains("10:COL=ColumnOrLocal(VARIABLE@6:2)")
    }

    @Test
    fun valueCategoriesFollowTheDeclaredType() {
        val text = """
            declare
              r t%rowtype;
              type rec is record (a number, b varchar2(10));
              rr rec;
              type outer_rec is record (r t%rowtype, k number);
              x outer_rec;
              cursor c is select a, b from t;
              rc c%rowtype;
              subtype row_t is t%rowtype;
              rs row_t;
              ra r%type;
              type aa is table of t%rowtype index by pls_integer;
              va aa;
              type nt is table of number;
              vn nt;
              type vr is varray(3) of rec;
              vv vr;
              n number;
              o my_object;
            begin
              insert into t values r;
              insert into t values rr;
              insert into t values x.r;
              insert into t values rc;
              insert into t values rs;
              insert into t values ra;
              insert into t values va(1);
              insert into t values vn(1);
              insert into t values vv(1);
              insert into t values n;
              insert into t values o;
              insert into t values unknown_name;
              for q in (select * from t) loop
                insert into t values q;
              end loop;
            end;
            /
            """.trimIndent() + "\n"
        assertThat(valueCategories(text)).containsExactly(
            "21:r=Record(ROWTYPE)", "22:rr=Record(RECORD_TYPE)", "23:x=Record(ROWTYPE)", "24:rc=Record(ROWTYPE)",
            "25:rs=Record(ROWTYPE)", "26:ra=Record(ROWTYPE)",
            "27:va=Collection(ASSOCIATIVE_ARRAY of Record(ROWTYPE))", "28:vn=Collection(NESTED_TABLE of Scalar)",
            "29:vv=Collection(VARRAY of Record(RECORD_TYPE))", "30:n=Scalar",
            // A schema type, or a name, the file does not declare and no project describes.
            "31:o=Unknown[DECLARATION_OUTSIDE_FILE]", "32:unknown_name=Unknown[DECLARATION_OUTSIDE_FILE]",
            "34:q=Record(ROWTYPE)"
        )
        val spec = "create package pkg is\n  pr t%rowtype;\n  procedure p;\nend;\n/\n"
        val body = "create package body pkg is\n  procedure p is\n  begin\n    insert into t values pr;\n    insert into t values pkg.pr;\n  end;\nend;\n/\n"
        assertThat(valueCategories(body, "s.sql" to spec, prepared = true)).containsExactly("4:pr=Record(ROWTYPE)", "5:pkg=Record(ROWTYPE)")
        assertThat(valueCategories(body, "s.sql" to spec))
            .containsExactly("4:pr=Unknown[DECLARATION_OUTSIDE_FILE]", "5:pkg=Unknown[DECLARATION_OUTSIDE_FILE]")
        val cc = "create procedure p is\n  r t%rowtype;\n  \$if \$\$x \$then r number; \$end\nbegin\n  insert into t values r;\nend;\n/\n"
        assertThat(valueCategories(cc)).containsExactly("5:r=Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun aSysRefcursorIsACursorVariable() {
        val text = "declare\n  rc sys_refcursor;\n  subtype cur is sys_refcursor;\n  rs cur;\nbegin\n  insert into t values rc;\n  insert into t values rs;\nend;\n/\n"
        assertThat(valueCategories(text)).containsExactly("6:rc=CursorVariable", "7:rs=CursorVariable")
        // A local declaration of the name decides.
        val local = "declare\n  type sys_refcursor is record (a number);\n  rc sys_refcursor;\nbegin\n  insert into t values rc;\nend;\n/\n"
        assertThat(valueCategories(local)).containsExactly("5:rc=Record(RECORD_TYPE)")
    }

    private fun valueCategories(text: String, vararg others: Pair<String, String>, prepared: Boolean = false): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                for (values in node.getDescendants(DmlGrammar.VALUES_CLAUSE)) {
                    val value = values.lastChild
                    val name = if (value.type === PlSqlGrammar.METHOD_CALL) value.firstChild else value
                    rows += "${value.token.line}:${name.tokenOriginalValue}=${this.context.valueCategory(name)}"
                }
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    @Test
    fun aNameInARegionTheAnalysisCannotSee() {
        val text = "declare\n  v date;\n  \$if \$\$x \$then sysdate date; \$end\nbegin\n  v := sysdate;\nend;\n/\n"
        assertThat(nameBindings(text)).containsExactly(
            "5:V=Unknown[CONDITIONAL_COMPILATION]", "5:SYSDATE=Unknown[CONDITIONAL_COMPILATION]"
        )
    }

    private fun nameBindings(text: String, vararg others: Pair<String, String>, prepared: Boolean = false): List<String> {
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation().prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val bindings = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME)
                    .forEach { bindings += "${it.token.line}:${it.tokenValue}=${this.context.nameBinding(it)}" }
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return bindings
    }

    @Test
    fun aSpecificationVariableInAnotherFile() {
        val spec = "create package p as\n  counter number;\n  procedure run;\nend;\n/\n"
        val body = "create package body p as\n  procedure run is\n    counter number;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(hiding(body, "s.sql" to spec)).containsExactly("COUNTER: Hides(s.sql:2:2)")
        assertThat(hiding(body, prepared = false)).containsExactly("COUNTER: Unknown[DECLARATION_OUTSIDE_FILE]")
        // A body declaration is a duplicate of the specification's, not hiding: the same answer with or without the project.
        val duplicate = "create package body p as\n  counter number;\n  procedure run is begin null; end;\nend;\n/\n"
        assertThat(hiding(duplicate, "s.sql" to spec)).containsExactly("COUNTER: HidesNothing")
        assertThat(hiding(duplicate, prepared = false)).containsExactly("COUNTER: HidesNothing")
    }

    @Test
    fun aSpecificationVariableIsVisibleOutsideTheFile() {
        assertThat(evidence("create package p as\n  g number;\nend;\n/\n")).containsExactly("G: Unknown[VISIBLE_OUTSIDE_FILE]")
    }

    @Test
    fun theProjectCoverageMakesAUseInABodyCertain() {
        val spec = "create package p as\n  procedure run;\nend;\n/\n"
        val body = "create package body p as\n  counter number;\n  procedure run is begin counter := 1; end;\nend;\n/\n"
        // Without the project, the specification in another file may declare the name too.
        assertThat(evidence(body, prepared = false)).containsExactly("COUNTER: Unknown[DECLARATION_OUTSIDE_FILE]")
        assertThat(evidence(body, "s.sql" to spec)).containsExactly("COUNTER: Used")
    }

    @Test
    fun noEvidenceWithoutTheLocalAnalysisOrForAnotherNode() {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse("declare\n  v number;\nbegin\n  null;\nend;\n/\n")
        val declaration = root.getFirstDescendant(PlSqlGrammar.VARIABLE_DECLARATION)!!
        val context = PlSqlVisitorContext(root, Source("u.sql", ""), null)
        assertThat(context.usageEvidence(declaration)).isEqualTo(UsageEvidence.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))

        val rows = ArrayList<UsageEvidence>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                rows += this.context.usageEvidence(node.getFirstDescendant(PlSqlGrammar.STATEMENTS_SECTION)!!)
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", "declare\n  v number;\nbegin\n  null;\nend;\n/\n"), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        assertThat(rows).containsExactly(UsageEvidence.Unavailable(UsageUnavailability.NOT_A_COVERED_DECLARATION))
    }
}
