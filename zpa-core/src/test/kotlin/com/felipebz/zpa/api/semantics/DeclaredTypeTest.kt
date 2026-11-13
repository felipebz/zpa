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
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.parser.PlSqlParser
import com.felipebz.zpa.project.FileId
import com.felipebz.zpa.project.ProjectAnalysisContext
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/** What a written datatype declares, and the type reference the project retains for it. */
class DeclaredTypeTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val spec = "create package pk_cfg as\n" +
        "  c_width constant pls_integer := 40;\n" +
        "  c_calc constant pls_integer := c_width * 2;\n" +
        "  type t_ids is table of number index by pls_integer;\n" +
        "  type t_names is table of varchar2(20);\n" +
        "  type t_codes is varray(5) of char(2);\n" +
        "  subtype t_amount is number(8,2);\n" +
        "  subtype t_num is number;\n" +
        "  subtype t_short is t_num(5,1);\n" +
        "  g_amt t_num(6,2);\n" +
        "  g_cv t_amount(4,1);\n" +
        "  g_copy g_amt%type;\n" +
        "  g_wide t_num(c_width, 2);\n" +
        "  g_label varchar2(25 char);\n" +
        "  g_sized varchar2(c_width);\n" +
        "  procedure log_it(p_msg varchar2);\n" +
        "end;\n/\n"

    private val body = "create package body pk_cfg as\n" +
        "  v_private varchar2(c_width);\n" +
        "  procedure log_it(p_msg varchar2) is begin null; end;\n" +
        "end;\n/\n"

    private val function = "create function fn_code(p_id number) return varchar2 is\nbegin\n  return null;\nend;\n/\n"

    private val schemaType = "create type t_list as table of number;\n/\n"

    private val user = "create procedure u_demo is\n" +
        "  c_size constant pls_integer := 30;\n" +
        "  c_twice constant pls_integer := c_size * 2;\n" +
        "  c_dyn constant pls_integer := length('abc');\n" +
        "  v_a varchar2(30);\n" +
        "  v_b varchar2(30 char);\n" +
        "  v_c number(15,3);\n" +
        "  v_d number(*,2);\n" +
        "  v_e number;\n" +
        "  v_f varchar2(c_size);\n" +
        "  v_g varchar2(c_twice);\n" +
        "  v_h varchar2(c_dyn);\n" +
        "  v_i varchar2(pk_cfg.c_width);\n" +
        "  v_u varchar2(pk_cfg.c_calc);\n" +
        "  v_j timestamp(3) with time zone;\n" +
        "  v_k interval day(2) to second(6);\n" +
        "  type r_row is record (id number(10), name varchar2(c_size));\n" +
        "  type t_aa is table of r_row index by pls_integer;\n" +
        "  type t_nt is table of r_row;\n" +
        "  type t_va is varray(10) of r_row;\n" +
        "  v_aa t_aa;\n" +
        "  v_nt t_nt;\n" +
        "  v_va t_va;\n" +
        "  v_r r_row;\n" +
        "  subtype s_base is number(8,2);\n" +
        "  subtype s_money is s_base;\n" +
        "  v_l s_money;\n" +
        "  v_total number(12,2);\n" +
        "  v_copy v_total%type;\n" +
        "  v_copy2 v_copy%type;\n" +
        "  v_m pk_cfg.t_ids;\n" +
        "  v_w pk_cfg.t_codes;\n" +
        "  v_x pk_cfg.t_names;\n" +
        "  v_n t_rel%rowtype;\n" +
        "  v_o t_rel.col_a%type;\n" +
        "  v_p pk_cfg.t_amount;\n" +
        "  v_q pk_cfg.g_label%type;\n" +
        "  v_s t_list;\n" +
        "  subtype s_n is number;\n" +
        "  subtype s_vc is varchar2;\n" +
        "  subtype s_c is varchar2(10);\n" +
        "  subtype s_d is number(8,2);\n" +
        "  subtype s_iv is interval day(2) to second(6);\n" +
        "  subtype s_ts is timestamp;\n" +
        "  v_y s_n(6,2);\n" +
        "  v_z s_vc(20);\n" +
        "  v_pp pk_cfg.t_num(6,2);\n" +
        "  v_zc v_z%type;\n" +
        "  v_wide s_c(20);\n" +
        "  v_narrow s_d(6,2);\n" +
        "  v_sized s_vc(c_size);\n" +
        "  v_ts s_ts(3);\n" +
        "  v_iv s_iv(3);\n" +
        "  v_ps pk_cfg.t_short;\n" +
        "  v_pa pk_cfg.g_amt%type;\n" +
        "  v_pg pk_cfg.g_cv%type;\n" +
        "  v_pc pk_cfg.g_copy%type;\n" +
        "  v_pw pk_cfg.g_wide%type;\n" +
        "begin\n" +
        "  declare\n" +
        "    type t_list is table of date;\n" +
        "    v_t t_list;\n" +
        "  begin\n" +
        "    null;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val ordSpec = "create package pk_ord as\n" +
        "  type t_line is record (line_no number(4), qty number(9,2), note varchar2(40));\n" +
        "  type t_outer is record (inner t_line, tag varchar2(5));\n" +
        "  g_line t_line;\n" +
        "  g_outer t_outer;\n" +
        "  g_cust customer%rowtype;\n" +
        "  procedure run;\n" +
        "end;\n/\n"

    private val ordBody = "create package body pk_ord as\n" +
        "  procedure run is\n" +
        "    b_qty g_line.qty%type;\n" +
        "    b_deep g_outer.inner.qty%type;\n" +
        "    b_name g_cust.name%type;\n" +
        "    b_nope g_line.nope%type;\n" +
        "  begin\n" +
        "    null;\n" +
        "  end;\n" +
        "end;\n/\n"

    private val ordUse = "create procedure p_use is\n" +
        "  v_qty pk_ord.g_line.qty%type;\n" +
        "  v_deep pk_ord.g_outer.inner.qty%type;\n" +
        "  v_name pk_ord.g_cust.name%type;\n" +
        "  v_nope pk_ord.g_line.nope%type;\n" +
        "begin\n" +
        "  null;\n" +
        "end;\n/\n"

    private val sources = mapOf(
        "cfg.pks" to spec, "cfg.pkb" to body, "fn.sql" to function, "type.sql" to schemaType, "u.sql" to user,
        "ord.pks" to ordSpec, "ord.pkb" to ordBody, "use.sql" to ordUse
    )

    private val prepared = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))

    private fun render(type: DeclaredType, context: PlSqlVisitorContext): String = when (type) {
        is DeclaredType.Record -> "Record(${type.fields.joinToString { it.name }})"
        is DeclaredType.Collection -> "Collection(${type.kind} of ${type.element?.let { render(context.declaredType(it), context) }})"
        is DeclaredType.Project -> "Project(${type.declaration.qualifiedName}" +
            ((type.declaration as? ProjectShapedTypeView)?.collectionKind?.map { " $it" }?.orElse("") ?: "") + ")"
        else -> type.toString()
    }

    /** `name=answer` for each written type of a variable, parameter, record field or RETURN of [file]. */
    private fun scan(file: String, context: ProjectAnalysisContext = prepared, answer: (PlSqlVisitorContext, AstNode) -> String): List<String> {
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter { it.parent.type === PlSqlGrammar.VARIABLE_DECLARATION || it.parent.type === PlSqlGrammar.PARAMETER_DECLARATION ||
                        it.parent.type === PlSqlGrammar.CREATE_FUNCTION || it.parent.type === PlSqlGrammar.RECORD_FIELD_DECLARATION
                }.forEach {
                    val name = it.parent.getFirstChildOrNull(PlSqlGrammar.IDENTIFIER_NAME)?.tokenOriginalValue ?: "return"
                    rows += "$name=${answer(this.context, it)}"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    private fun declared(file: String = "u.sql") = scan(file) { context, node -> render(context.declaredType(node), context) }.associate { it.substringBefore('=') to it.substringAfter('=') }

    @Test
    fun predefinedTypesWithTheirWrittenConstraints() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                "v_a" to "BuiltIn(VARCHAR2(length=30))",
                "v_b" to "BuiltIn(VARCHAR2(length=30 CHAR))",
                "v_c" to "BuiltIn(NUMBER(precision=15, scale=3))",
                "v_d" to "BuiltIn(NUMBER(precision=Maximum, scale=2))",
                "v_e" to "BuiltIn(NUMBER())",
                "v_j" to "BuiltIn(TIMESTAMP WITH TIME ZONE(scale=3))",
                "v_k" to "BuiltIn(INTERVAL DAY TO SECOND(precision=2, scale=6))"
            )
        )
    }

    @Test
    fun constraintsWrittenAsConstants() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                // Local constants, one of them computed from another.
                "v_f" to "BuiltIn(VARCHAR2(length=30))",
                "v_g" to "BuiltIn(VARCHAR2(length=60))",
                "name" to "BuiltIn(VARCHAR2(length=30))",
                // A default that is not static: present, not guessed.
                "v_h" to "BuiltIn(VARCHAR2(length=Unknown))",
                // A package constant of another file with an integer literal default; one computed from another is not kept.
                "v_i" to "BuiltIn(VARCHAR2(length=40))",
                "v_u" to "BuiltIn(VARCHAR2(length=Unknown))"
            )
        )
        // The specification's constant, named alone from its package body in another file.
        assertThat(declared("cfg.pkb")).containsEntry("v_private", "BuiltIn(VARCHAR2(length=40))")
    }

    @Test
    fun typesOfThisFile() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                "v_r" to "Record(id, name)",
                // Each element node is described in turn.
                "v_aa" to "Collection(ASSOCIATIVE_ARRAY of Record(id, name))",
                "v_nt" to "Collection(NESTED_TABLE of Record(id, name))",
                "v_va" to "Collection(VARRAY of Record(id, name))",
                // A local type hides the schema type of the same name.
                "v_s" to "Project(t_list NESTED_TABLE)",
                "v_t" to "Collection(NESTED_TABLE of BuiltIn(DATE()))"
            )
        )
    }

    @Test
    fun subtypesAndTypeAnchorsAreFollowed() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                "v_l" to "BuiltIn(NUMBER(precision=8, scale=2))",
                "v_copy" to "BuiltIn(NUMBER(precision=12, scale=2))",
                "v_copy2" to "BuiltIn(NUMBER(precision=12, scale=2))",
                // Through the project: a package subtype and a package variable of another file.
                "v_p" to "BuiltIn(NUMBER(precision=8, scale=2))",
                "v_q" to "BuiltIn(VARCHAR2(length=25 CHAR))"
            )
        )
    }

    @Test
    fun constraintsWrittenWhereASubtypeIsUsed() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                "v_y" to "BuiltIn(NUMBER(precision=6, scale=2))",
                "v_z" to "BuiltIn(VARCHAR2(length=20))",
                "v_pp" to "BuiltIn(NUMBER(precision=6, scale=2))",
                "v_zc" to "BuiltIn(VARCHAR2(length=20))",
                // The constraints written at the use replace a constrained subtype's, wider or narrower.
                "v_wide" to "BuiltIn(VARCHAR2(length=20))",
                "v_narrow" to "BuiltIn(NUMBER(precision=6, scale=2))",
                "v_sized" to "BuiltIn(VARCHAR2(length=30))",
                "v_ts" to "BuiltIn(TIMESTAMP(scale=3))",
                // Which INTERVAL field one written value constrains is not decided here.
                "v_iv" to "BuiltIn(INTERVAL DAY TO SECOND(precision=Unknown, scale=Unknown))",
                // A subtype of another file constrained where it uses another subtype.
                "v_ps" to "BuiltIn(NUMBER(precision=5, scale=1))",
                // Package variables of another file typed by a constrained subtype use, directly or through another one.
                "v_pa" to "BuiltIn(NUMBER(precision=6, scale=2))",
                "v_pg" to "BuiltIn(NUMBER(precision=4, scale=1))",
                "v_pc" to "BuiltIn(NUMBER(precision=6, scale=2))",
                // A constant of another file is not valued there.
                "v_pw" to "BuiltIn(NUMBER(precision=Unknown, scale=2))"
            )
        )
    }

    @Test
    fun typesAndAnchorsOfOtherFiles() {
        assertThat(declared()).containsAllEntriesOf(
            mapOf(
                "v_m" to "Project(pk_cfg.t_ids ASSOCIATIVE_ARRAY)",
                "v_w" to "Project(pk_cfg.t_codes VARRAY)",
                "v_x" to "Project(pk_cfg.t_names NESTED_TABLE)",
                // Without external metadata the relation's columns are not known: the anchor is reported.
                "v_n" to "Anchored(t_rel%ROWTYPE)",
                "v_o" to "Anchored(t_rel.col_a%TYPE)"
            )
        )
        val categories = scan("u.sql") { context, node -> context.typeCategory(node).toString() }
        assertThat(categories).contains("v_m=Collection(ASSOCIATIVE_ARRAY of Scalar(NUMERIC))", "v_s=Collection(NESTED_TABLE of Scalar(NUMERIC))")
    }

    @Test
    fun theReferenceTheProjectRetains() {
        fun retained(file: String) = scan(file) { context, node -> context.projectTypeReference(node)?.let { "${it.name}${it.constraints.orElse(null)}" }.toString() }
        assertThat(retained("cfg.pks")).containsExactly(
            "c_width=pls_integer()", "c_calc=pls_integer()",
            // A subtype's name, or an anchor, has no predefined type's constraints to read as written.
            "g_amt=t_numnull", "g_cv=t_amountnull", "g_copy=g_amtnull", "g_wide=t_numnull",
            "g_label=varchar2(length=25 CHAR)",
            // The index keeps no constant's value.
            "g_sized=varchar2(length=Unknown)", "p_msg=varchar2()"
        )
        // A variable of the body is not indexed.
        assertThat(retained("cfg.pkb")).containsExactly("v_private=null", "p_msg=varchar2()")
        assertThat(retained("fn.sql")).containsExactly("p_id=number()", "return=varchar2()")
    }

    @Test
    fun aFileThatWasNotAnalyzed() {
        val context = PlSqlVisitorContext(PlSqlParser.create(PlSqlConfiguration(StandardCharsets.UTF_8, false)).parse(user), null, null)
        val datatype = context.rootTree()!!.getFirstDescendant(PlSqlGrammar.DATATYPE)
        assertThat(context.declaredType(datatype)).isEqualTo(DeclaredType.Unavailable(UsageUnavailability.FILE_NOT_ANALYZED))
        assertThat(context.projectTypeReference(datatype)).isNull()
    }

    @Test
    fun aNodeThatIsNotADatatype() {
        val answers = scan("u.sql") { context, node -> context.declaredType(node.parent).toString() }
        assertThat(answers).allMatch { it.endsWith("=Unavailable(NOT_A_COVERED_NAME)") }
    }

    @Test
    fun fieldsOfPackageRecordValues() {
        val customer = ProjectExternalRelation.of(
            "SALES", "CUSTOMER", true, listOf(
                ProjectExternalColumn.of("CUSTOMER_ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 7, 0)),
                ProjectExternalColumn.of("NAME", 2, ProjectExternalDatatype.of("VARCHAR2", null, 60, null, null, null))
            )
        )
        val metadata = ProjectExternalMetadata.builder("fixture").completeSchema("SALES").relation(customer).build()
        val withMetadata = prepared.withExternalContext(ProjectExternalContext.of(metadata, ProjectIdentifier.fromSource("SALES")))
        for ((file, prefix) in listOf("use.sql" to "v_", "ord.pkb" to "b_")) {
            val types = scan(file) { context, node -> render(context.declaredType(node), context) }.toMap()
            // Through the record value to its field, also when the field is itself a record.
            assertThat(types["${prefix}qty"]).isEqualTo("BuiltIn(NUMBER(precision=9, scale=2))")
            assertThat(types["${prefix}deep"]).isEqualTo("BuiltIn(NUMBER(precision=9, scale=2))")
            // A field of a %ROWTYPE value is the column's %TYPE of its relation.
            assertThat(types["${prefix}name"]).isEqualTo("Anchored(customer.name%TYPE)")
            val categories = scan(file) { context, node -> context.typeCategory(node).toString() }.toMap()
            assertThat(categories["${prefix}qty"]).isEqualTo("Scalar(NUMERIC)")
            assertThat(categories["${prefix}name"]).doesNotStartWith("Record")

            val described = scan(file, withMetadata) { context, node ->
                val description = (context.declaredType(node) as? DeclaredType.Anchored)?.description
                val column = description?.lookup?.flatMap { it.column }?.flatMap { it.datatype }?.map { "${it.name}(${it.length.asInt})" }?.orElse("")
                "${description?.status} $column"
            }.toMap()
            assertThat(described["${prefix}name"]).isEqualTo("RESOLVED VARCHAR2(60)")
            // A field the record type does not have: the project reports the anchor it rejects.
            val missing = scan(file) { context, node -> ((context.declaredType(node) as? DeclaredType.Anchored)?.description?.source?.anchorStop?.orElse(null)).toString() }.toMap()
            assertThat(missing["${prefix}nope"]).isEqualTo("FIELD_NOT_FOUND")
        }
    }

    private fun List<String>.toMap() = associate { it.substringBefore('=') to it.substringAfter('=') }
}
