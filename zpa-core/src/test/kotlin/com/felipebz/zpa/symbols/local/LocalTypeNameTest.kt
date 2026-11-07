//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

@file:OptIn(ZpaExperimentalApi::class)

package com.felipebz.zpa.symbols.local

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

/** The declaration a written type name denotes, as the local type walk answers it. */
class LocalTypeNameTest {
    private class Source(private val name: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = name
        override fun path(): Path = Path.of(name)
        override fun type() = PlSqlFile.Type.MAIN
    }

    private val packageSpec = "create package p as\n" +
        "  type t_rec is record (n number);\n" +
        "  subtype t_alias is t_rec;\n" +
        "end;\n/\n"

    private val types = "create type t_obj as object (a number);\n/\n" +
        "create type t_twice as object (a number);\n/\n"

    private val twice = "create type t_twice as object (b number);\n/\n"

    private val user = "create procedure u is\n" +
        "  v p.t_rec;\n" +
        "  o t_obj;\n" +
        "  s p.t_alias;\n" +
        "  w t_twice;\n" +
        "  n t_none;\n" +
        "  type lr is record (x number);\n" +
        "  l lr;\n" +
        "  i pls_integer;\n" +
        "begin\n  null;\nend;\n/\n"

    private fun names(file: String, sources: Map<String, String>): List<String> =
        written(file, sources) { it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION }

    /** `line:type=name|category` for each `DATATYPE` of [file] that [select] keeps. */
    private fun written(
        file: String,
        sources: Map<String, String>,
        withCategory: Boolean = false,
        unreadable: List<String> = emptyList(),
        select: (AstNode) -> Boolean
    ): List<String> {
        val inputs = sources.map { (id, text) -> ProjectSource(FileId(id)) { text } } +
            unreadable.map { id -> ProjectSource(FileId(id)) { throw java.io.IOException("unreadable") } }
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(inputs, concurrent = false))
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            override fun leaveFile(node: AstNode) {
                node.getDescendants(PlSqlGrammar.DATATYPE).filter(select).forEach {
                    val category = if (withCategory) "|${this.context.valueCategories!!.typeCategory(it)}" else ""
                    rows += "${it.token.line}:${it.tokens.joinToString("") { t -> t.originalValue }}=${this.context.valueCategories!!.typeName(it)}$category"
                }
            }
        }
        AstScanner(emptyList(), null, false, StandardCharsets.UTF_8, context).scanFile(Source(file, sources.getValue(file)), listOf(check), FileId(file))
        return rows
    }

    @Test
    fun theFirstDeclarationAWrittenTypeNameReaches() {
        val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "w.sql" to twice, "u.sql" to user)
        assertThat(names("u.sql", sources)).containsExactly(
            "2:p.t_rec=Project(PACKAGE_TYPE p.t_rec)",
            "3:t_obj=Project(STANDALONE_TYPE t_obj)",
            // A subtype in another file is the declaration the name denotes, not the record type it is a subtype of.
            "4:p.t_alias=Project(PACKAGE_SUBTYPE p.t_alias)",
            // Two schema types of the name in the project: no one declaration.
            "5:t_twice=Unresolved(AMBIGUOUS)",
            "6:t_none=Unresolved(NOT_FOUND_IN_PROJECT)",
            "8:lr=Local(D6)",
            "9:pls_integer=BuiltIn(pls_integer)"
        )
    }

    @Test
    fun conditionalCompilationLeavesTheNameUndecided() {
        // A dropped branch that ends a block it does not open may restructure the unit: every name stays undecided.
        val conditional = user.replace("  type lr is record (x number);\n", "  \$if \$\$flag \$then type t_rec is record (y number); \$else end; \$end\n  type lr is record (x number);\n")
        val sources = mapOf("p.sql" to packageSpec, "t.sql" to types, "u.sql" to conditional)
        assertThat(names("u.sql", sources)).allMatch { it.endsWith("Undecided[CONDITIONAL_COMPILATION]") }
        // Text that only declares `t_rec` keeps undecided the names that write it, and no other.
        val narrow = mapOf("p.sql" to packageSpec, "t.sql" to types, "u.sql" to conditional.replace("\$else end; ", ""))
        assertThat(names("u.sql", narrow).filter { "CONDITIONAL_COMPILATION" in it }).allMatch { it.contains("t_rec") }
    }

    private val schemaTypes = "create type t_base as object (a number) not final;\n/\n" +
        "create type t_sub under t_base (b number);\n/\n" +
        "create type t_nt as table of number;\n/\n" +
        "create package pk_t as\n  type t_ptab is table of number;\n  type t_prec is record (n number);\n  subtype t_ps is varchar2(10);\nend;\n/\n"

    /**
     * The `DATATYPE` nodes outside declarations: what expressions, SQL columns and external parameters write. With
     * [conditional], an undecided `$IF` among the first unit's declarations: one whose dropped branch ends a block it
     * does not open (it may restructure the unit), or with [wide] false one that only declares `y`.
     */
    private fun expressions(text: String, conditional: Boolean = false, wide: Boolean = true): List<String> {
        val selection = if (wide) "\$if \$\$flag \$then y number; \$else end; \$end" else "\$if \$\$flag \$then y number; \$end"
        val body = if (conditional) text.replace(" is\n", " is\n  $selection\n") else text
        return written("u.sql", mapOf("s.sql" to schemaTypes, "u.sql" to body), withCategory = true) {
            it.parent.type != PlSqlGrammar.VARIABLE_DECLARATION && it.parent.type != PlSqlGrammar.CUSTOM_SUBTYPE &&
                it.parent.type != PlSqlGrammar.NESTED_TABLE_DEFINITION &&
                it.parent.type != PlSqlGrammar.RECORD_FIELD_DECLARATION
        }
    }

    @Test
    fun typesInPlSqlExpressionsFollowPlSqlScope() {
        val text = "create procedure u is\n" +
            "  o t_base;\n  s t_sub;\n  subtype ls is t_sub;\n  x varchar2(10);\n  type lr is record (n number);\n  r lr;\n" +
            "begin\n" +
            "  s := treat(o as t_sub);\n" +
            "  s := treat(o as ls);\n" +
            "  if o is of (t_sub) then null; end if;\n" +
            "  x := cast(1 as varchar2);\n" +
            "  x := cast('a' as ls);\n" +
            "  o := cast(null as t_base);\n" +
            "end;\n/\n"
        assertThat(expressions(text)).containsExactly(
            "9:t_sub=Project(STANDALONE_TYPE t_sub)|ObjectInstance",
            // A local subtype of an object type: Oracle takes it in TREAT and IS OF in PL/SQL.
            "10:ls=Local(D3)|ObjectInstance",
            "11:t_sub=Project(STANDALONE_TYPE t_sub)|ObjectInstance",
            "12:varchar2=BuiltIn(varchar2)|Scalar(CHARACTER)",
            // CAST in PL/SQL takes a predefined or a schema type, not a local subtype (PLS-00382).
            "13:ls=Unresolved(NOT_FOUND_IN_PROJECT)|Unknown[DECLARATION_OUTSIDE_FILE]",
            "14:t_base=Project(STANDALONE_TYPE t_base)|ObjectInstance"
        )
        assertThat(expressions(text, conditional = true)).allMatch { it.contains("CONDITIONAL_COMPILATION") }
    }

    @Test
    fun typesInSqlSeeSchemaTypesOnly() {
        val text = "create procedure u is\n" +
            "  type t_nt is table of varchar2(5);\n  subtype ls is t_sub;\n  n number;\n" +
            "begin\n" +
            "  select count(*) into n from table(cast(multiset(select 1 from dual) as t_nt));\n" +
            "  select count(*) into n from table(cast(multiset(select 1 from dual) as pk_t.t_ptab));\n" +
            "  select treat(t_sub(1, 2) as t_sub).b into n from dual;\n" +
            "  select count(*) into n from dual where t_sub(1, 2) is of (ls);\n" +
            "  select count(*) into n from xmltable('/a' passing xmltype('<a>1</a>') columns c t_nt path '.', d number path '.');\n" +
            "end;\n/\n"
        assertThat(expressions(text)).containsExactly(
            // A local type of the name does not hide the schema type in SQL.
            "6:t_nt=Project(STANDALONE_TYPE t_nt)|Collection(? of Scalar(NUMERIC))",
            // SQL does not see a package type (ORA-00902).
            "7:pk_t.t_ptab=Undecided[UNSUPPORTED_REFERENCE]|Unknown[UNSUPPORTED_REFERENCE]",
            "8:t_sub=Project(STANDALONE_TYPE t_sub)|ObjectInstance",
            // Nor a local subtype (ORA-00902).
            "9:ls=Unresolved(NOT_FOUND_IN_PROJECT)|Unknown[DECLARATION_OUTSIDE_FILE]",
            "10:t_nt=Project(STANDALONE_TYPE t_nt)|Collection(? of Scalar(NUMERIC))",
            "10:number=BuiltIn(number)|Scalar(NUMERIC)"
        )
        assertThat(expressions(text, conditional = true)).allMatch { it.contains("CONDITIONAL_COMPILATION") }
    }

    @Test
    fun indexByKeysRefCursorReturnsTableColumnsAndExternalParameters() {
        val text = "create procedure u is\n" +
            "  type ta is table of number index by pk_t.t_ps;\n" +
            "  type rc is ref cursor return pk_t.t_prec;\n" +
            "begin\n  null;\nend;\n/\n" +
            "create table line_tab (o t_base, p pk_t.t_prec);\n" +
            "create function ext(n pls_integer) return pls_integer as language c library lib name \"ext\" parameters (n int);\n/\n"
        assertThat(expressions(text)).contains(
            "2:pk_t.t_ps=Project(PACKAGE_SUBTYPE pk_t.t_ps)|Scalar(CHARACTER)",
            "3:pk_t.t_prec=Project(PACKAGE_TYPE pk_t.t_prec)|Record(RECORD_TYPE)",
            "8:t_base=Project(STANDALONE_TYPE t_base)|ObjectInstance",
            "8:pk_t.t_prec=Undecided[UNSUPPORTED_REFERENCE]|Unknown[UNSUPPORTED_REFERENCE]",
            // A C datatype of a call specification: never looked up.
            "9:int=BuiltIn(int)|Unknown[UNSUPPORTED_REFERENCE]"
        )
        assertThat(expressions(text, conditional = true).filter { it.startsWith("2:") || it.startsWith("3:") }).allMatch { it.contains("CONDITIONAL_COMPILATION") }
        // A `${'$'}IF` that writes neither `pk_t` nor the types leaves these lookups decided.
        assertThat(expressions(text, conditional = true, wide = false).filter { it.startsWith("2:") || it.startsWith("3:") }).noneMatch { it.contains("CONDITIONAL_COMPILATION") }
    }

    @Test
    fun sysRefcursorIsPredefinedUnlessALocalTypeTakesTheName() {
        val predefined = "create procedure u is\n  c sys_refcursor;\nbegin\n  null;\nend;\n/\n"
        assertThat(names("u.sql", mapOf("u.sql" to predefined))).containsExactly("2:sys_refcursor=BuiltIn(sys_refcursor)")
        val local = "create procedure u is\n  type sys_refcursor is ref cursor;\n  c sys_refcursor;\nbegin\n  null;\nend;\n/\n"
        assertThat(names("u.sql", mapOf("u.sql" to local))).containsExactly("3:sys_refcursor=Local(D1)")
    }

    @Test
    fun typeNamesInAPackageBodyWhoseSpecificationIsInAnotherFile() {
        val spec = "create package pk_s as\n  type t_rec is record (n number);\n  type t_obj is record (n number);\nend;\n/\n"
        val schema = "create type t_obj as object (a number);\n/\ncreate type t_other as object (a number);\n/\n"
        val body = "create package body pk_s as\n" +
            "  procedure run is\n" +
            "    a t_rec;\n" +
            "    b t_obj;\n" +
            "    c t_other;\n" +
            "  begin\n    null;\n  end;\n" +
            "  procedure run2 is\n" +
            "    type t_other is record (x number);\n" +
            "    d t_other;\n" +
            "  begin\n    null;\n  end;\n" +
            "end;\n/\n"
        assertThat(names("b.sql", mapOf("s.sql" to spec, "t.sql" to schema, "b.sql" to body))).containsExactly(
            // Unqualified, looked up from the package: its specification's type.
            "3:t_rec=Project(PACKAGE_TYPE pk_s.t_rec)",
            // A package type of the name wins over the schema type.
            "4:t_obj=Project(PACKAGE_TYPE pk_s.t_obj)",
            "5:t_other=Project(STANDALONE_TYPE t_other)",
            // A local type of the name hides the schema type after its declaration.
            "11:t_other=Local(D6)"
        )
    }

    @Test
    fun theOwnNameInATypeBodyIsTheType() {
        val spec = "create type t_self as object (n number, member procedure p);\n/\n"
        val body = "create type body t_self as\n  member procedure p is\n    v t_self;\n  begin\n    null;\n  end;\nend;\n/\n"
        assertThat(names("b.sql", mapOf("s.sql" to spec, "b.sql" to body))).containsExactly("3:t_self=Project(STANDALONE_TYPE t_self)")
    }

    @Test
    fun anIncompleteIndexNeverResolvesAProjectName() {
        // A file that could not be read may declare the same names, so even a name the index holds once is not resolved.
        val rows = written("u.sql", mapOf("p.sql" to packageSpec, "u.sql" to user), unreadable = listOf("broken.sql")) {
            it.parent.type == PlSqlGrammar.VARIABLE_DECLARATION
        }
        assertThat(rows).contains(
            "2:p.t_rec=Unresolved(INCOMPLETE_INDEX)",
            "6:t_none=Unresolved(INCOMPLETE_INDEX)",
            "9:pls_integer=BuiltIn(pls_integer)"
        )
    }
}
