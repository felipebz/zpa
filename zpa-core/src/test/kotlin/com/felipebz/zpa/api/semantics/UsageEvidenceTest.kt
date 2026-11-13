//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.DmlGrammar
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.PlSqlVisitorContext
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.project.ProjectExternalContext
import com.felipebz.zpa.api.project.ProjectExternalMetadata
import com.felipebz.zpa.api.project.ProjectSchemaPaths
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.*
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
        // The outer block's conditional text writes `y`: it may change the pair.
        val text = "declare\n  y number;\nbegin\n  \$if \$\$a \$then y := 1; \$end\n  declare\n    y number;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(hiding(text)).containsExactly("Y: HidesNothing", "Y: Unknown[CONDITIONAL_COMPILATION]")
        // Text that never writes `y` cannot declare it nor hide it.
        assertThat(hiding(text.replace("y := 1;", "null;"))).containsExactly("Y: HidesNothing", "Y: Hides(2:2)")
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
            // The first name of a chain: nothing of the file is named `x`.
            "12:X=NothingLocal"
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
            "21:r=Record(ROWTYPE, Named(t))", "22:rr=Record(RECORD_TYPE)", "23:x=Record(ROWTYPE, Named(t))", "24:rc=Record(ROWTYPE, QueryColumns, [A, B])",
            "25:rs=Record(ROWTYPE, Named(t))", "26:ra=Record(ROWTYPE, Named(t))",
            "27:va=Collection(ASSOCIATIVE_ARRAY of Record(ROWTYPE, Named(t)))", "28:vn=Collection(NESTED_TABLE of Scalar(NUMERIC))",
            "29:vv=Collection(VARRAY of Record(RECORD_TYPE))", "30:n=Scalar(NUMERIC)",
            // A schema type, or a name, the file does not declare and no project describes.
            "31:o=Unknown[DECLARATION_OUTSIDE_FILE]", "32:unknown_name=Unknown[DECLARATION_OUTSIDE_FILE]",
            "34:q=Record(ROWTYPE, Named(t))"
        )
        val spec = "create package pkg is\n  pr t%rowtype;\n  procedure p;\nend;\n/\n"
        val body = "create package body pkg is\n  procedure p is\n  begin\n    insert into t values pr;\n    insert into t values pkg.pr;\n  end;\nend;\n/\n"
        assertThat(valueCategories(body, "s.sql" to spec, prepared = true)).containsExactly("4:pr=Record(ROWTYPE, Named(t))", "5:pkg=Record(ROWTYPE, Named(t))")
        assertThat(valueCategories(body, "s.sql" to spec))
            .containsExactly("4:pr=Unknown[DECLARATION_OUTSIDE_FILE]", "5:pkg=Unknown[DECLARATION_OUTSIDE_FILE]")
        val cc = "create procedure p is\n  r t%rowtype;\n  \$if \$\$x \$then r number; \$end\nbegin\n  insert into t values r;\nend;\n/\n"
        assertThat(valueCategories(cc)).containsExactly("5:r=Unknown[CONDITIONAL_COMPILATION]")
    }

    @Test
    fun theRowOfACursor() {
        // With a RETURN type, c%ROWTYPE is still the query's row (fields b, a), and a loop over c has the RETURN type's.
        val local = "create procedure p is\n  cursor c return tab%rowtype is select b, a from tab;\n  r c%rowtype;\nbegin\n" +
            "  insert into tab values r;\n  for r2 in c loop\n    insert into tab values r2;\n  end loop;\nend;\n/\n"
        assertThat(valueCategories(local)).containsExactly("5:r=Record(ROWTYPE, QueryColumns, [B, A])", "7:r2=Record(ROWTYPE, Named(tab))")
        // A package cursor's row is not a relation: no relation is known, its select list names its fields.
        val spec = "create package cur_pkg is\n  cursor c is select b, a from tab;\nend;\n/\n"
        val user = "create procedure u is\n  r cur_pkg.c%rowtype;\nbegin\n  insert into tab values r;\nend;\n/\n"
        assertThat(valueCategories(user, "s.sql" to spec, prepared = true)).containsExactly("4:r=Record(ROWTYPE, [B, A])")
    }

    @Test
    fun aPackageCursorOfTheUnitsSchema() {
        // Oracle reads pk3.c from a unit of the schema APP as app.pk3.c, a cursor.
        val spec = "create package app.pk3 is\n  cursor c is select b, a from tab;\nend;\n/\n"
        val mapped = "create procedure u is\n  r pk3.c%rowtype;\nbegin\n  insert into tab values r;\nend;\n/\n"
        assertThat(valueCategories(mapped, "s.sql" to spec, prepared = true, schemaPaths = "u.sql=APP")).containsExactly("4:r=Record(ROWTYPE, [B, A])")
        val owned = "create procedure app.u is\n  r pk3.c%rowtype;\nbegin\n  insert into tab values r;\nend;\n/\n"
        assertThat(valueCategories(owned, "s.sql" to spec, prepared = true)).containsExactly("4:r=Record(ROWTYPE, [B, A])")
    }

    @Test
    fun elementsOfCollectionsTheProjectDescribes() {
        val spec = "create package pkg is\n  type rec is record (a number);\n  type recs is table of rec;\n  type nums is table of number;\n" +
            "  type rows is table of t%rowtype;\n  v_recs recs;\n  v_nums nums;\n  v_rows rows;\n  procedure p;\nend;\n/\n"
        val type = "create type str_list as table of varchar2(4000);\n/\n"
        val body = "create package body pkg is\n  procedure p is\n    l_list str_list;\n    l_other other_schema.str_list;\n  begin\n" +
            "    insert into t values v_recs(1);\n    insert into t values v_nums(1);\n    insert into t values v_rows(1);\n" +
            "    insert into t values l_list(1);\n    insert into t values l_other(1);\n  end;\nend;\n/\n"
        // A schema-qualified type the project cannot place stays unknown.
        assertThat(valueCategories(body, "s.sql" to spec, "type.sql" to type, prepared = true)).containsExactly(
            "6:v_recs=Collection(NESTED_TABLE of Record(RECORD_TYPE))", "7:v_nums=Collection(NESTED_TABLE of Scalar(NUMERIC))", "8:v_rows=Collection(NESTED_TABLE of Record(ROWTYPE, Named(t)))",
            "9:l_list=Collection(NESTED_TABLE of Scalar(CHARACTER))", "10:l_other=Unknown[DECLARATION_OUTSIDE_FILE]"
        )
    }

    @Test
    fun cyclicCollectionTypes() {
        val types = "create type a as table of a;\n/\ncreate type b as table of c;\n/\ncreate type c as table of b;\n/\n"
        val proc = "create procedure p is\n  la a;\n  lb b;\n  type x is table of a;\n  lx x;\nbegin\n" +
            "  insert into t values la;\n  insert into t values lb;\n  insert into t values lx;\nend;\n/\n"
        // A collection reached again while its own element is being classified is unknown, as in the file.
        assertThat(valueCategories(proc, "types.sql" to types, prepared = true)).containsExactly(
            "7:la=Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE])",
            "8:lb=Collection(NESTED_TABLE of Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE]))",
            "9:lx=Collection(NESTED_TABLE of Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE]))"
        )
        val spec = "create package pkg is\n  type s is table of s;\n  type x is table of y;\n  type y is table of x;\n  vs s;\n  vx x;\n  procedure p;\nend;\n/\n"
        val body = "create package body pkg is\n  procedure p is\n  begin\n    insert into t values vs;\n    insert into t values vx;\n  end;\nend;\n/\n"
        assertThat(valueCategories(body, "s.sql" to spec, prepared = true)).containsExactly(
            "4:vs=Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE])", "5:vx=Collection(NESTED_TABLE of Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE]))"
        )
        assertThat(valueCategories("declare\n  type x is table of x;\n  l x;\nbegin\n  insert into t values l;\nend;\n/\n"))
            .containsExactly("5:l=Collection(NESTED_TABLE of Unknown[UNSUPPORTED_REFERENCE])")
    }

    @Test
    fun qualifiedNamesAndWrittenTypes() {
        val text = "create function f(p number) return date is\n  subtype amount is number;\n  v amount;\n  a v%type;\n  d date;\n" +
            "  i interval day to second;\n  b boolean;\n  c varchar2(10);\n  type rec is record (x number);\n  r rec;\nbegin\n" +
            "  v := f.p;\n  v := r.x;\n  v := pkg.x;\n  return d;\nend;\n/\n"
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).filter { it.parent.type !== PlSqlGrammar.CUSTOM_DATATYPE && it.parent.type !== PlSqlGrammar.ANCHORED_DATATYPE }
                    .forEach { rows += "${it.token.line}:${it.tokenOriginalValue}=${this.context.nameBinding(it)}" }
                node.getDescendants(PlSqlGrammar.DATATYPE).forEach { rows += "${it.token.line}:${it.tokenOriginalValue}=${this.context.typeCategory(it)}" }
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        // `f.p` through the function's name; a record field is not a declaration; `pkg` is not in the file.
        assertThat(rows).containsExactly(
            "12:f=Local(PARAMETER@1:18)", "13:r=Unknown[UNSUPPORTED_REFERENCE]", "14:pkg=NothingLocal",
            "1:number=Scalar(NUMERIC)", "1:date=Scalar(DATETIME)", "2:number=Scalar(NUMERIC)", "3:amount=Scalar(NUMERIC)",
            "4:v=Scalar(NUMERIC)", "5:date=Scalar(DATETIME)", "6:interval=Scalar(INTERVAL)", "7:boolean=Scalar(BOOLEAN)",
            "8:varchar2=Scalar(CHARACTER)", "9:number=Scalar(NUMERIC)", "10:rec=Record(RECORD_TYPE)"
        )
    }

    @Test
    fun aSysRefcursorIsACursorVariable() {
        val text = "declare\n  rc sys_refcursor;\n  subtype cur is sys_refcursor;\n  rs cur;\nbegin\n  insert into t values rc;\n  insert into t values rs;\nend;\n/\n"
        assertThat(valueCategories(text)).containsExactly("6:rc=CursorVariable", "7:rs=CursorVariable")
        // A local declaration of the name decides.
        val local = "declare\n  type sys_refcursor is record (a number);\n  rc sys_refcursor;\nbegin\n  insert into t values rc;\nend;\n/\n"
        assertThat(valueCategories(local)).containsExactly("5:rc=Record(RECORD_TYPE)")
    }

    private fun valueCategories(text: String, vararg others: Pair<String, String>, prepared: Boolean = false, schemaPaths: String = ""): List<String> {
        val schemas = ProjectExternalContext.of(ProjectExternalMetadata.none(), null, ProjectSchemaPaths.parse(null, schemaPaths))
        val project = if (!prepared) ProjectAnalysisContext.NOT_PREPARED else ProjectAnalysisContext.prepared(
            ProjectIndexPreparation.withSchemas(ProjectDeclarationExtractor(), schemas)
                .prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
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
        // The conditional declaration writes `sysdate`, not `v`.
        val text = "declare\n  v date;\n  \$if \$\$x \$then sysdate date; \$end\nbegin\n  v := sysdate;\nend;\n/\n"
        assertThat(nameBindings(text)).containsExactly(
            "5:V=Local(VARIABLE@2:2)", "5:SYSDATE=Unknown[CONDITIONAL_COMPILATION]"
        )
    }

    /** A package body whose `p1` holds a `$IF` among its statements, and a query in `p2`. */
    private val ccInP1 = "create or replace package body pack_cc is\n  procedure p1 is\n  begin\n" +
        "    \$IF \$\$flag_a \$THEN null; \$ELSE null; \$END\n    null;\n  end;\n" +
        "  procedure p2 is\n    v number;\n  begin\n    select order_total into v from shop_orders;\n  end;\nend;\n/\n"

    @Test
    fun aDroppedBranchInOneSubprogramMayDeclareASubprogramOfThePackageBody() {
        // Oracle accepts the body with `$ELSE null; end; function order_total return number is begin return 1;`: under
        // those settings p1 ends early and the package body declares `order_total`, visible in p2. So it stays unknown.
        val closing = ccInP1.replace("\$ELSE null; \$END", "\$ELSE null; end; function order_total return number is begin return 1; \$END")
        assertThat(nameBindings(closing)).anyMatch { it.startsWith("10:ORDER_TOTAL=Unknown[") && it.contains("CONDITIONAL_COMPILATION") }
        // A dropped branch that ends no block it does not open declares nothing p2 sees.
        assertThat(nameBindings(ccInP1)).anyMatch { it.startsWith("10:ORDER_TOTAL=") && "CONDITIONAL_COMPILATION" !in it }
    }

    @Test
    fun aSelectionInOneUnitLeavesTheOtherUnitsOfTheFileAlone() {
        val procedure = "create or replace procedure proc_cc is\nbegin\n  \$IF \$\$flag_a \$THEN null; \$ELSE null; \$END\n  null;\nend;\n/\n"
        val block = "declare\n  v number;\nbegin\n  select order_total into v from shop_orders;\nend;\n/\n"
        val function = "create or replace function fn_total(p_amount amount_t) return number is\nbegin\n  return 1;\nend;\n/\n"
        // Each CREATE and each top-level block is compiled on its own: the answers are those of the file without proc_cc.
        assertThat(nameBindings(procedure + block)).contains("10:ORDER_TOTAL=NothingLocal(column possible)")
        assertThat(typeCategories(procedure + function).map { it.substringAfter(':') }).isEqualTo(typeCategories(function).map { it.substringAfter(':') })
        assertThat(typeCategories(procedure + function)).noneMatch { it.contains("CONDITIONAL_COMPILATION") }
    }

    @Test
    fun aDroppedDeclarationOfThePackageBodyKeepsTheNameUnknown() {
        val text = "create or replace package body pack_cc is\n  \$IF \$\$flag_a \$THEN\n  \$ELSE\n    order_total number;\n  \$END\n" +
            "  procedure p2 is\n    v number;\n  begin\n    select order_total into v from shop_orders;\n  end;\nend;\n/\n"
        assertThat(nameBindings(text)).anyMatch { it.startsWith("9:ORDER_TOTAL=Unknown[") && it.contains("CONDITIONAL_COMPILATION") }
    }

    private fun typeCategories(text: String): List<String> {
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).forEach { rows += "${it.token.line}:${it.tokenOriginalValue}=${this.context.typeCategory(it)}" }
            }
        }
        val project = ProjectAnalysisContext.NOT_PREPARED
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    /** `line:column:name=binding` for each name written in a qualified name of [text], the project prepared with [text] alone. */
    private fun chainBindings(text: String): List<String> {
        val project = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(listOf(ProjectSource(FileId("u.sql")) { text }), concurrent = false))
        val root = PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(text)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.MEMBER_EXPRESSION).flatMap { it.getChildren(PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME) }
                    .forEach { rows += "${it.token.line}:${it.token.column}:${it.tokenValue}=${this.context.nameBinding(it)}" }
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return rows
    }

    private val chains = "create or replace package acct as\n  procedure run(p_rec in out nocopy acct_rec_t);\nend;\n/\n" +
        "create or replace package body acct as\n" +
        "  type rec_t is record (f number, g varchar2(10));\n" +
        "  type tab_t is table of rec_t index by pls_integer;\n" +
        "  type num_tab is table of number;\n" +
        "  g_rec rec_t;\n" +
        "  g_tab tab_t;\n" +
        "  g_nums num_tab := num_tab();\n" +
        "  g_x number;\n" +
        "  procedure take(o out number) is begin o := 1; end;\n" +
        "  procedure run(p_rec in out nocopy acct_rec_t) is\n" +
        "    l_rec rec_t;\n" +
        "    n number;\n" +
        "  begin\n" +
        "    n := g_rec.f;\n" +
        "    g_rec.f := 1;\n" +
        "    n := g_tab.count;\n" +
        "    if g_tab.exists(3) then null; end if;\n" +
        "    g_tab.delete;\n" +
        "    g_nums.extend;\n" +
        "    g_nums.delete(1);\n" +
        "    take(g_rec.f);\n" +
        "    select 1 into g_rec.f from dual;\n" +
        "    n := acct.g_x;\n" +
        "    n := acct.g_rec.f;\n" +
        "    n := g_tab(1).f;\n" +
        "    l_rec.f := g_rec.f;\n" +
        "    n := p_rec.f;\n" +
        "    for i in 1 .. g_tab.count loop null; end loop;\n" +
        "    insert into t values (g_rec.f);\n" +
        "    n := other_pkg.v.f;\n" +
        "    declare\n" +
        "      g_rec rec_t;\n" +
        "    begin\n" +
        "      n := g_rec.f;\n" +
        "    end;\n" +
        "  end;\nend;\n/\n"

    @Test
    fun theFirstNameOfAChainBindsItsDeclaration() {
        val rec = "Local(VARIABLE@9:2)"
        val tab = "Local(VARIABLE@10:2)"
        val nums = "Local(VARIABLE@11:2)"
        val member = "Unavailable(NOT_A_COVERED_NAME)"
        assertThat(chainBindings(chains)).containsExactly(
            // Read, assignment target, collection method, method call statements.
            "18:9:G_REC=$rec", "18:15:F=$member",
            "19:4:G_REC=$rec", "19:10:F=$member",
            "20:9:G_TAB=$tab", "20:15:COUNT=$member",
            "21:7:G_TAB=$tab", "21:13:EXISTS=$member",
            "22:4:G_TAB=$tab", "22:10:DELETE=$member",
            "23:4:G_NUMS=$nums", "23:11:EXTEND=$member",
            "24:4:G_NUMS=$nums", "24:11:DELETE=$member",
            // An OUT actual, an INTO target.
            "25:9:G_REC=$rec", "25:15:F=$member",
            "26:18:G_REC=$rec", "26:24:F=$member",
            // Through the package's own name: the package, then the variable.
            "27:9:ACCT=Local(OTHER@5:31)", "27:14:G_X=Local(VARIABLE@12:2)",
            "28:9:ACCT=Local(OTHER@5:31)", "28:14:G_REC=$rec", "28:20:F=$member",
            // `g_tab(1).f` keeps its answer on the call's name, not listed here: a local, a global.
            "30:4:L_REC=Local(VARIABLE@15:4)", "30:10:F=$member",
            "30:15:G_REC=$rec", "30:21:F=$member",
            // A parameter whose type the analysis cannot see.
            "31:9:P_REC=Local(PARAMETER@14:16)", "31:15:F=$member",
            "32:18:G_TAB=$tab", "32:24:COUNT=$member",
            "33:26:G_REC=$rec", "33:32:F=$member",
            // Nothing of the file is named `other_pkg`.
            "34:9:OTHER_PKG=NothingLocal", "34:19:V=NothingLocal", "34:21:F=NothingLocal",
            // An inner declaration hides the body's.
            "38:11:G_REC=Local(VARIABLE@36:6)", "38:17:F=$member"
        )
    }

    @Test
    fun aRelationOfTheStatementTakesTheFirstNameOfAChainBeforeAVariable() {
        val text = "create or replace package shop as\n  procedure run;\nend;\n/\n" +
            "create or replace package body shop as\n  type rec_t is record (f number);\n  g_rec rec_t;\n  procedure run is\n    n number;\n  begin\n" +
            "    select g_rec.f into n from shop_orders g_rec;\n    select g_rec.f into n from shop_orders;\n  end;\nend;\n/\n"
        // The alias takes `g_rec.f` when its table has the column f, else the record does. With no relation of that name,
        // `g_rec.f` cannot be a column: an object column's attribute needs the table alias.
        assertThat(chainBindings(text)).containsExactly(
            "11:11:G_REC=ColumnOrLocal(VARIABLE@7:2)", "11:17:F=Unavailable(NOT_A_COVERED_NAME)",
            "12:11:G_REC=Local(VARIABLE@7:2)", "12:17:F=Unavailable(NOT_A_COVERED_NAME)"
        )
    }

    @Test
    fun theFirstNameOfAChainIsUnknownWhereAPlainNameIs() {
        val text = "create or replace package shop_cc as\n  procedure run;\nend;\n/\n" +
            "create or replace package body shop_cc as\n  type rec_t is record (f number);\n  \$if \$\$flag_a \$then g_cc rec_t; \$end\n" +
            "  procedure run is\n    n number;\n  begin\n    n := g_cc.f;\n    n := g_cc;\n  end;\nend;\n/\n"
        assertThat(chainBindings(text)).containsExactly(
            "11:9:G_CC=Unknown[CONDITIONAL_COMPILATION]", "11:14:F=Unknown[CONDITIONAL_COMPILATION]"
        )
        assertThat(nameBindings(text)).contains("12:G_CC=Unknown[CONDITIONAL_COMPILATION]")
    }

    /** `line:NAME=binding` of each plain name of [text], lexed with [settings], the project prepared with [others] and [text] lexed alike. */
    private fun nameBindingsWith(settings: ConditionalCompilationSettings, text: String, vararg others: Pair<String, String>): List<String> {
        val configuration = PlSqlConfiguration(StandardCharsets.UTF_8, false, settings)
        val project = ProjectAnalysisContext.prepared(
            ProjectIndexPreparation(ProjectDeclarationExtractor(configuration))
                .prepare((others.toList() + ("u.sql" to text)).map { (id, t) -> ProjectSource(FileId(id)) { t } }, concurrent = false)
        )
        val root = PlSqlParser.create(configuration).parse(text)
        val bindings = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.VARIABLE_NAME).forEach { bindings += "${it.token.line}:${it.tokenValue}=${this.context.nameBinding(it)}" }
            }
        }
        PlSqlAstWalker(SemanticAnalysisPipeline(project).create(FileId("u.sql")).all + check)
            .walk(PlSqlVisitorContext(root, Source("u.sql", text), null), SemanticScanCapabilities(createProjectAnalysis(project)))
        return bindings
    }

    private val versionIn19 = ConditionalCompilationSettings(databaseVersion = DatabaseVersion(19, 0))

    private val versionTest = "create or replace package pack_cc is\n  procedure p1;\n  procedure p2;\nend;\n/\n" +
        "create or replace package body pack_cc is\n  g_total number;\n  procedure p1 is\n  begin\n" +
        "    \$IF dbms_db_version.ver_le_10 \$THEN\n      null;\n    \$ELSE\n      g_total := 0;\n    \$END\n  end;\n" +
        "  procedure p2 is\n  begin\n    g_total := g_total + 1;\n  end;\nend;\n/\n"

    @Test
    fun aSelectionTheSettingsDecideAddsNoUncertainty() {
        // Version 19: `ver_le_10` is FALSE, Oracle compiles `g_total := 0;`, as the analysis does.
        assertThat(nameBindingsWith(versionIn19, versionTest)).containsExactly(
            "13:G_TOTAL=Local(VARIABLE@7:2)", "18:G_TOTAL=Local(VARIABLE@7:2)", "18:G_TOTAL=Local(VARIABLE@7:2)"
        )
        // The same file with the directives and the dropped branch removed, lines kept.
        val replaced = versionTest.replace("    \$IF dbms_db_version.ver_le_10 \$THEN\n      null;\n    \$ELSE\n      g_total := 0;\n    \$END\n", "\n\n\n      g_total := 0;\n\n")
        assertThat(nameBindingsWith(versionIn19, versionTest)).isEqualTo(nameBindings(replaced, prepared = true))
        // Without settings the kept branch is `null;`; the uses in p2 are decided: p1's statements declare nothing.
        assertThat(nameBindingsWith(ConditionalCompilationSettings.NONE, versionTest)).containsExactly(
            "18:G_TOTAL=Local(VARIABLE@7:2)", "18:G_TOTAL=Local(VARIABLE@7:2)"
        )
    }

    @Test
    fun aFlagTheSettingsGiveOrLeaveUnsetDecidesTheSelection() {
        val text = "declare\n  v number;\nbegin\n  \$IF \$\$trace_on \$THEN\n    v := 1;\n  \$ELSE\n    v := 2;\n  \$END\n  v := v + 1;\nend;\n/\n"
        val set = nameBindingsWith(ConditionalCompilationSettings(mapOf("trace_on" to CcValue.True)), text)
        // Flags configured without `trace_on`: NULL, so not TRUE.
        val unset = nameBindingsWith(ConditionalCompilationSettings(mapOf("other_flag" to CcValue.Integer(1))), text)
        assertThat(set).containsExactly("5:V=Local(VARIABLE@2:2)", "9:V=Local(VARIABLE@2:2)", "9:V=Local(VARIABLE@2:2)")
        assertThat(unset).containsExactly("7:V=Local(VARIABLE@2:2)", "9:V=Local(VARIABLE@2:2)", "9:V=Local(VARIABLE@2:2)")
    }

    @Test
    fun aConditionTheSettingsCannotDecideAmongAnotherSubprogramsStatementsChangesNothing() {
        val text = versionTest.replace("dbms_db_version.ver_le_10", "pack_cfg.c_on")
        // An undecided selection among p1's statements, whose dropped branch ends no block, changes nothing in p2.
        assertThat(nameBindingsWith(versionIn19, text)).containsExactly("18:G_TOTAL=Local(VARIABLE@7:2)", "18:G_TOTAL=Local(VARIABLE@7:2)")
    }

    @Test
    fun aSpecificationInAnotherFileWithADecidedSelection() {
        val spec = "create or replace package pack_doc is\n  \$IF dbms_db_version.ver_le_10 \$THEN\n    g_size number;\n  \$ELSE\n    g_size pls_integer;\n  \$END\n" +
            "  procedure run;\nend;\n/\n"
        val plain = "create or replace package pack_doc is\n  g_size pls_integer;\n  procedure run;\nend;\n/\n"
        val body = "create or replace package body pack_doc is\n  procedure run is\n    n number;\n  begin\n    select g_size into n from shop_orders;\n  end;\nend;\n/\n"
        val decided = nameBindingsWith(versionIn19, body, "s.sql" to spec)
        assertThat(decided).contains("5:G_SIZE=ColumnOrLocal(VARIABLE@s.sql:5:4)")
        // The same answer as with the selection written out, but for the declaration's place.
        assertThat(nameBindingsWith(versionIn19, body, "s.sql" to plain)).contains("5:G_SIZE=ColumnOrLocal(VARIABLE@s.sql:2:2)")
        assertThat(nameBindingsWith(ConditionalCompilationSettings.NONE, body, "s.sql" to spec)).noneMatch { it.startsWith("5:G_SIZE=ColumnOrLocal") }
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
    private val sqlCallSpec =
        "create or replace package pk_lines is\n" +
        "  type t_nums is table of number;\n" +
        "  type t_row is record (items t_nums);\n" +
        "  type t_rows is table of t_row index by pls_integer;\n" +
        "  procedure run(p_rows t_rows, p_rec t_row);\n" +
        "end pk_lines;\n" +
        "/\n"

    private val sqlCallBody =
        "create or replace package body pk_lines is\n" +
        "  procedure run(p_rows t_rows, p_rec t_row) is\n" +
        "    l number;\n" +
        "    l_nums t_nums := t_nums(7);\n" +
        "    l_row t_row;\n" +
        "  begin\n" +
        "    insert into line_tab (x) values (p_rows(1).items(1));\n" +
        "    insert into line_tab (x) values (p_rec.items(1));\n" +
        "    insert into line_tab (x) values (l_nums(1));\n" +
        "    insert into line_tab (x) values (l_row.items(1));\n" +
        "    select count(*) into l from line_tab where x = p_rec.items(1);\n" +
        "    update line_tab set x = l_nums(1) where x = p_rows(1).items(1);\n" +
        "    select p_rec.items(1) into l from dual;\n" +
        "    l := p_rows(1).items(1);\n" +
        "    l := p_rec.items(1);\n" +
        "  end;\n" +
        "end pk_lines;\n" +
        "/\n"

    @Test
    fun aValueWithAnArgumentListInStaticSqlBindsAsInPlSql() {
        // The SQL statements of the body; INTO targets and columns are left out.
        val rows = nameBindings(sqlCallBody, "s.sql" to sqlCallSpec, prepared = true)
            .filter { it.substringBefore(':').toInt() in 7..13 && !it.startsWith("${it.substringBefore(':')}:X=") && !it.contains(":L=") }
        val pRows = "Local(PARAMETER@2:16)"
        val pRec = "Local(PARAMETER@2:31)"
        assertThat(rows).containsExactly(
            // A field after an element is not a declaration of the file.
            "7:P_ROWS=$pRows", "7:ITEMS=Unavailable(NOT_A_COVERED_NAME)",
            "8:P_REC=$pRec",
            "9:L_NUMS=Local(VARIABLE@4:4)",
            "10:L_ROW=Local(VARIABLE@5:4)",
            "11:COUNT=Unavailable(NOT_A_COVERED_NAME)", "11:P_REC=$pRec",
            "12:L_NUMS=Local(VARIABLE@4:4)", "12:P_ROWS=$pRows", "12:ITEMS=Unavailable(NOT_A_COVERED_NAME)",
            "13:P_REC=$pRec"
        )
    }

    private val sqlCallUtil =
        "create or replace package pk_lines_util is\n" +
        "  function f(p number) return number;\n" +
        "end pk_lines_util;\n" +
        "/\n"

    private val sqlCallEdges =
        "create or replace procedure use_lines is\n" +
        "  type t_nums is table of number;\n" +
        "  l_nums t_nums := t_nums(1);\n" +
        "  upper t_nums := t_nums(2);\n" +
        "  l number;\n" +
        "  c clob;\n" +
        "  l_xml xmltype;\n" +
        "begin\n" +
        "  select l_nums(1) into l from line_tab l_nums;\n" +
        "  select count(*) into l from line_tab l_nums where x in (select l_nums(1) from dual);\n" +
        "  select l_xml.doc.getclobval() into c from xml_tab l_xml;\n" +
        "  select l_xml.getclobval() into c from dual;\n" +
        "  select pk_lines_util.f(l_nums(1)) into l from dual;\n" +
        "  select fn_schema(l_nums(1)) into l from dual;\n" +
        "  select upper(1), nvl(l, 1) into l, l from dual;\n" +
        "end;\n" +
        "/\n" +
        "create or replace procedure use_cc is\n" +
        "  type t_nums is table of number;\n" +
        "  \$if \$\$flag \$then\n" +
        "  l_c t_nums := t_nums(1);\n" +
        "  \$end\n" +
        "  l number;\n" +
        "begin\n" +
        "  select l_c(1) into l from dual;\n" +
        "end;\n" +
        "/\n"

    @Test
    fun sqlTakesANameWithAnArgumentListBeforeALocalValue() {
        val rows = nameBindings(sqlCallEdges, "util.sql" to sqlCallUtil, prepared = true)
            .filter { it.substringBefore(':').toInt() >= 9 && !it.contains(":L=") && !it.contains(":C=") && !it.contains(":X=") }
        val lNums = "Local(VARIABLE@3:2)"
        assertThat(rows).containsExactly(
            // Neither a relation of the statement nor one of an enclosing query takes `v(1)`: Oracle reads the collection.
            "9:L_NUMS=$lNums",
            "10:COUNT=Unavailable(NOT_A_COVERED_NAME)", "10:L_NUMS=$lNums",
            // The alias takes the first name of an object column's method; without one, the local object does.
            "11:L_XML=NothingLocal(column possible)",
            "12:L_XML=Local(VARIABLE@7:2)",
            // A package or schema function keeps no answer; its argument is the local collection.
            "13:PK_LINES_UTIL=Unavailable(NOT_A_COVERED_NAME)", "13:L_NUMS=$lNums",
            "14:FN_SCHEMA=Unavailable(NOT_A_COVERED_NAME)", "14:L_NUMS=$lNums",
            // A SQL built-in wins over a local collection of the same name.
            "15:UPPER=Unavailable(NOT_A_COVERED_NAME)", "15:NVL=Unavailable(NOT_A_COVERED_NAME)",
            "21:T_NUMS=Unavailable(NOT_A_COVERED_NAME)", "21:T_NUMS=Unknown[CONDITIONAL_COMPILATION]",
            // A declaration under an undecided `${'$'}IF` leaves the name undecided.
            "25:L_C=Unknown[CONDITIONAL_COMPILATION]"
        )
    }

    @Test
    fun aBuiltInDocumentedUnderAnotherEntryWinsOverALocalCollection() {
        val text = "create or replace procedure check_lines is\n" +
            "  type t_nums is table of number;\n" +
            "  lengthb t_nums := t_nums(9);\n" +
            "  substrb t_nums := t_nums(10);\n" +
            "  l varchar2(100);\n" +
            "begin\n" +
            "  select lengthb('abc'), substrb('abc', 1) into l, l from dual;\n" +
            "end;\n/\n"
        // Oracle runs LENGTHB and SUBSTRB, not the collections.
        assertThat(nameBindings(text).filter { it.startsWith("7:") && !it.contains(":L=") })
            .containsExactly("7:LENGTHB=Unavailable(NOT_A_COVERED_NAME)", "7:SUBSTRB=Unavailable(NOT_A_COVERED_NAME)")
    }

    @Test
    fun theTypesOfSpecificationHeadingsAreCategorizedAsInABody() {
        val spec = "create or replace package p as\n" +
            "  type t_rec is record (n number);\n" +
            "  g_n number;\n" +
            "  procedure run(a number, b varchar2, c t_rec, d g_n%type);\n" +
            "  function total(a number) return varchar2;\n" +
            "end;\n/\n"
        val body = "create or replace package body p as\n" +
            "  procedure run(a number, b varchar2, c t_rec, d g_n%type) is begin null; end;\n" +
            "  function total(a number) return varchar2 is begin return null; end;\n" +
            "end;\n/\n"
        val expected = listOf(
            "number=Scalar(NUMERIC)", "varchar2=Scalar(CHARACTER)", "t_rec=Record(RECORD_TYPE)", "g_n=Scalar(NUMERIC)",
            "number=Scalar(NUMERIC)", "varchar2=Scalar(CHARACTER)"
        )
        val rows = typeCategories(spec + body).map { it.substringBefore(':').toInt() to it.substringAfter(':') }
        // The field's and the variable's types come first; then the heading (lines 4 and 5) and the body (lines 9 and 10).
        assertThat(rows.filter { it.first in 4..5 }.map { it.second }).isEqualTo(expected)
        assertThat(rows.filter { it.first in 9..10 }.map { it.second }).isEqualTo(expected)
    }

    @Test
    fun theParametersOfObjectTypeMethodHeadingsAreCategorized() {
        val text = "create or replace type t_obj as object (\n  n number,\n" +
            "  member procedure put(p_value varchar2, p_when date),\n" +
            "  constructor function t_obj(p_n number) return self as result\n);\n/\n"
        assertThat(typeCategories(text)).containsExactly(
            "2:number=Scalar(NUMERIC)", "3:varchar2=Scalar(CHARACTER)", "3:date=Scalar(DATETIME)", "4:number=Scalar(NUMERIC)"
        )
    }
}
