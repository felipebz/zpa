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
package com.felipebz.zpa.project

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Declarations typed by a `%TYPE`/`%ROWTYPE` anchor on an object behind a database link (`t.c@link%TYPE`). */
class RemoteAnchorExtractionTest {
    private val fileId = FileId("remote.sql")
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(source: String) = extractor.extract(fileId, source)
    private fun spec(vararg members: String) = "CREATE PACKAGE p AS\n${members.joinToString("\n")}\nEND p;\n"

    /** Range of [text] in [source] (single line), at its first occurrence after a space or `(`. */
    private fun rangeOf(source: String, text: String): SourceRange {
        val at = listOf(" ", "(").map { source.indexOf(it + text) }.filter { it >= 0 }.minOrNull()!! + 1
        val line = source.substring(0, at).count { it == '\n' } + 1
        val column = at - (source.lastIndexOf('\n', at - 1) + 1)
        return SourceRange(fileId, line, column, line, column + text.length)
    }

    /** `name@link%ANCHOR`, the projection every assertion compares. */
    private fun shape(ref: TypeRef?): String? = when (ref) {
        null -> null
        is AnchoredTypeRef -> "${ref.name}${ref.link?.let { "@$it" } ?: ""}%${ref.anchor}"
        else -> ref.name.toString()
    }

    private fun assertAnchor(source: String, ref: TypeRef?, written: String, expected: String) {
        assertThat(shape(ref)).isEqualTo(expected)
        assertThat(ref!!.sourceRange).describedAs(written).isEqualTo(rangeOf(source, written))
    }

    @Test
    fun specValuesKeepTheAnchorAndItsLink() {
        val source = spec(
            "r1 s.t@l%rowtype;", "r2 t@l%ROWTYPE;", "v1 s.t.c@l%type;", "v2 t.c@l%type := null;",
            "v3 t.c@internet.intranet%TYPE;", "v4 t.c@\"LNK\"%TYPE;"
        )
        val values = extract(source).filterIsInstance<PackageValueDeclaration>()
        assertThat(values.map { it.name.originalSpelling }).containsExactly("r1", "r2", "v1", "v2", "v3", "v4")
        assertAnchor(source, values[0].typeRef, "s.t@l%rowtype", "s.t@l%ROWTYPE")
        assertAnchor(source, values[1].typeRef, "t@l%ROWTYPE", "t@l%ROWTYPE")
        assertAnchor(source, values[2].typeRef, "s.t.c@l%type", "s.t.c@l%TYPE")
        assertAnchor(source, values[3].typeRef, "t.c@l%type", "t.c@l%TYPE")
        assertAnchor(source, values[4].typeRef, "t.c@internet.intranet%TYPE", "t.c@internet.intranet%TYPE")
        assertAnchor(source, values[5].typeRef, "t.c@\"LNK\"%TYPE", "t.c@\"LNK\"%TYPE")
        assertThat(values[3].typeRef.name).isEqualTo(QualifiedName(listOf(OracleIdentifier.fromSource("t"), OracleIdentifier.fromSource("c"))))
        assertThat((values[4].typeRef as AnchoredTypeRef).link!!.segments.map { it.originalSpelling }).containsExactly("internet", "intranet")
        assertThat((values[5].typeRef as AnchoredTypeRef).link!!.last.quoted).isTrue()
        assertThat(extract(source).filterIsInstance<PackageDeclaration>().single().memberNames!!.unretained).isEmpty()
    }

    @Test
    fun subtypesRecordFieldsCollectionsAndCursors() {
        val source = spec(
            "subtype st is t.c@l%type;",
            "type rec is record (a t.c@l%type, b number);",
            "type t1 is table of s.t@l%rowtype;",
            "cursor cr return t@l%ROWTYPE;"
        )
        val facts = extract(source)
        assertAnchor(source, facts.filterIsInstance<PackageSubtypeDeclaration>().single().baseType, "t.c@l%type", "t.c@l%TYPE")
        val types = facts.filterIsInstance<PackageTypeDeclaration>()
        val rec = types.single { it.name.originalSpelling == "rec" }
        assertThat(rec.recordFields.map { "${it.ordinal}:${it.name.originalSpelling}:${shape(it.typeRef)}" })
            .containsExactly("0:a:t.c@l%TYPE", "1:b:number")
        assertAnchor(source, types.single { it.name.originalSpelling == "t1" }.elementType, "s.t@l%rowtype", "s.t@l%ROWTYPE")
        assertAnchor(source, facts.filterIsInstance<PackageCursorDeclaration>().single().returnType, "t@l%ROWTYPE", "t@l%ROWTYPE")
    }

    @Test
    fun specCallablesWithRemoteParametersAndReturns() {
        val source = spec(
            "function f(i in t.c@l%type) return t.d@l%type;",
            "procedure p(i in t.c@l%type default null);",
            "FUNCTION RETORNA_DS_ASSUNTOSOLIC(I_NR_SOLICITACAO IN SOLICSERVICO.NR_SOLICITACAO@internet.intranet%TYPE)\n" +
                "RETURN SOLICSERVICO.DS_ASSUNTO@internet.intranet%TYPE;"
        )
        val facts = extract(source)
        assertThat(facts.filterIsInstance<PackageDeclaration>().single().callableCoverage.omittedNames).isEmpty()
        val functions = facts.filterIsInstance<PackageFunctionDeclaration>()
        assertThat(functions.map { it.name.originalSpelling }).containsExactly("f", "RETORNA_DS_ASSUNTOSOLIC")
        assertAnchor(source, functions[0].parameters.single().typeRef, "t.c@l%type", "t.c@l%TYPE")
        assertAnchor(source, functions[0].returnType, "t.d@l%type", "t.d@l%TYPE")
        assertThat(shape(functions[1].returnType)).isEqualTo("SOLICSERVICO.DS_ASSUNTO@internet.intranet%TYPE")
        val procedure = facts.filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(procedure.parameters.single().defaultPresent).isTrue()
        assertThat(shape(procedure.parameters.single().typeRef)).isEqualTo("t.c@l%TYPE")
    }

    @Test
    fun aBodyFunctionWithARemoteReturnDoesNotSwallowLaterMembers() {
        val source = "CREATE PACKAGE BODY p AS\n" +
            "  function f2(i number) return t.d@l%type is begin return null; end;\n" +
            "  function f3 return number is begin return 1; end;\n" +
            "  procedure p4 is begin null; end;\n" +
            "begin\n  null;\nend p;\n"
        val facts = extract(source).filterIsInstance<PackageSubprogramDeclaration>()
        assertThat(facts.map { it.name.originalSpelling }).containsExactly("f2", "f3", "p4")
        assertAnchor(source, (facts[0] as PackageFunctionDeclaration).returnType, "t.d@l%type", "t.d@l%TYPE")
        assertThat(facts.map { it.role }).containsOnly(DeclarationRole.BODY)
    }

    @Test
    fun aBodyFunctionWithARejectedReturnIsSkippedAlone() {
        val source = "CREATE PACKAGE BODY p AS\n" +
            "  function bad return t@l is begin return null; end;\n" +
            "  function f3 return number is begin return 1; end;\n" +
            "  procedure p4 is begin null; end;\n" +
            "begin\n  null;\nend p;\n"
        assertThat(extract(source).filterIsInstance<PackageSubprogramDeclaration>().map { it.name.originalSpelling }).containsExactly("f3", "p4")
    }

    @Test
    fun standaloneFunctionWithRemoteTypes() {
        val source = "create function sf(i t.c@l%type) return t.d@l%type is begin return i; end;\n/\n" +
            "create procedure sp(i s.t@l%rowtype) is begin null; end;\n/\n"
        val facts = extract(source)
        val function = facts.filterIsInstance<StandaloneFunctionDeclaration>().single()
        assertAnchor(source, function.parameters.single().typeRef, "t.c@l%type", "t.c@l%TYPE")
        assertAnchor(source, function.returnType, "t.d@l%type", "t.d@l%TYPE")
        assertThat(shape(facts.filterIsInstance<StandaloneProcedureDeclaration>().single().parameters.single().typeRef)).isEqualTo("s.t@l%ROWTYPE")
    }

    @Test
    fun onlyTheAnchoredLinkShapeIsAccepted() {
        listOf(
            "g t@link;", "g SYS.X@link;", "g t.c@%type;", "g REF t@link%TYPE;", "g t.c@l x%TYPE;", "g t.c@l.%TYPE;",
            "g t.c@l%TYPE%TYPE;", "g t.c@l@m%TYPE;"
        ).forEach { declaration ->
            val values = extract(spec(declaration, "ok NUMBER;")).filterIsInstance<PackageValueDeclaration>()
            assertThat(values.map { it.name.originalSpelling }).describedAs(declaration).containsExactly("ok")
        }
    }

    @Test
    fun keysSeparateLinksAndCompareThemByIdentity() {
        fun key(type: String) = (extract(spec("v $type;")).filterIsInstance<PackageValueDeclaration>().single().typeRef).let {
            it.structuralKey() to it.headerIdentityKey()
        }
        val keys = listOf("t.c@a%type", "t.c@b%type", "t.c%type").map(::key)
        assertThat(keys.map { it.first }.toSet()).hasSize(3)
        assertThat(keys.map { it.second }.toSet()).hasSize(3)
        assertThat(key("T.C@LNK%TYPE")).isEqualTo(key("t.c@lnk%type"))
        assertThat(key("t.c@\"LNK\"%TYPE")).isEqualTo(key("t.c@lnk%type"))
        assertThat(key("t.c@l%rowtype")).isNotEqualTo(key("t.c@l%type"))
    }
}
