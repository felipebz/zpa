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

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.squid.AstScanner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ObjectTypeAttributeTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun prepare(vararg sources: Pair<String, String>) =
        ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))

    private fun type(context: ProjectAnalysisContext, name: String): StandaloneTypeDeclaration =
        (context.state as ProjectAnalysisContext.State.Prepared).result.index.declarations.filterIsInstance<StandaloneTypeDeclaration>()
            .single { it.name.last.lookupName == name.uppercase() }

    private fun member(context: ProjectAnalysisContext, typeName: String, member: String): String =
        when (val resolution = ProjectRecordMemberResolver(context).resolve(type(context, typeName), OracleIdentifier.fromSource(member))) {
            is ProjectRecordMemberResolution.Resolved -> "Resolved ${resolution.declaration.qualifiedName}.${resolution.field.name.originalSpelling}"
            else -> resolution.javaClass.simpleName
        }

    @Test
    fun attributesAreExtractedInOrderWithTypesAndRanges() {
        val type = extractor.extract(FileId("t.sql"), "CREATE OR REPLACE TYPE t AS OBJECT (\n  id NUMBER(10),\n  name VARCHAR2(30),\n" +
            "  MEMBER FUNCTION f RETURN NUMBER\n) NOT FINAL;").filterIsInstance<StandaloneTypeDeclaration>().single()
        assertThat(type.shape).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(type.attributes.map { "${it.ordinal} ${it.name.originalSpelling} ${it.typeRef.name}" }).containsExactly("0 id NUMBER", "1 name VARCHAR2")
        assertThat(type.attributes.map { it.sourceRange.startLine }).containsExactly(2, 3)
        assertThat(type.attributesComplete).isTrue()
        assertThat(type.supertype).isNull()
    }

    @Test
    fun aSubtypeInheritsItsSupertypesAttributes() {
        val context = prepare(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (a1 NUMBER, a2 VARCHAR2(10)) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (b1 DATE);"
        )
        val sub = type(context, "sub")
        assertThat(sub.shape).isEqualTo(ProjectTypeShape.OBJECT)
        assertThat(sub.supertype!!.name.toString()).isEqualToIgnoringCase("sup")
        assertThat(sub.attributes.map { it.name.originalSpelling }).containsExactly("b1")
        assertThat(member(context, "sub", "b1")).isEqualTo("Resolved sub.b1")
        assertThat(member(context, "sub", "a2")).isEqualTo("Resolved sup.a2")
        assertThat(member(context, "sub", "c")).isEqualTo("NotFound")
        val methods = prepare(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (a1 NUMBER, MEMBER FUNCTION total RETURN NUMBER) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (b1 DATE, STATIC PROCEDURE make);"
        )
        assertThat(member(methods, "sub", "total")).isEqualTo("Method")
        assertThat(member(methods, "sub", "make")).isEqualTo("Method")
        assertThat(member(methods, "sub", "other")).isEqualTo("NotFound")
    }

    @Test
    fun aMissingAttributeIsIncompleteWhenTheChainMayMissAttributes() {
        val orphan = prepare("sub.sql" to "CREATE TYPE sub UNDER missing_t (b1 DATE);")
        assertThat(member(orphan, "sub", "b1")).isEqualTo("Resolved sub.b1")
        assertThat(member(orphan, "sub", "x")).isEqualTo("Incomplete")
        val altered = prepare(
            "t.sql" to "CREATE TYPE t AS OBJECT (c1 NUMBER);",
            "alter.sql" to "ALTER TYPE t ADD ATTRIBUTE (c2 VARCHAR2(5)) CASCADE;"
        )
        assertThat(member(altered, "t", "c1")).isEqualTo("Resolved t.c1")
        assertThat(member(altered, "t", "c2")).isEqualTo("Incomplete")
        val sub = prepare("sup.sql" to "CREATE TYPE sup AS OBJECT (a1 NUMBER) NOT FINAL;", "sub.sql" to "CREATE TYPE sub UNDER sup (b1 DATE);")
        assertThat(ProjectRecordMemberResolver().resolve(type(sub, "sub"), OracleIdentifier.fromSource("a1")))
            .isInstanceOf(ProjectRecordMemberResolution.Incomplete::class.java)
    }

    @Test
    fun aDroppedAttributeIsNeverResolved() {
        val single = prepare("t.sql" to "CREATE TYPE t AS OBJECT (c1 NUMBER, c2 NUMBER);", "alter.sql" to "ALTER TYPE t DROP ATTRIBUTE c2 CASCADE;")
        assertThat(member(single, "t", "c2")).isEqualTo("Incomplete")
        assertThat(member(single, "t", "c1")).isEqualTo("Resolved t.c1")
        val list = prepare("t.sql" to "CREATE TYPE t AS OBJECT (c1 NUMBER, c2 NUMBER, c3 NUMBER);", "alter.sql" to "ALTER TYPE t DROP ATTRIBUTE (c1, c2) CASCADE;")
        assertThat(member(list, "t", "c1")).isEqualTo("Incomplete")
        assertThat(member(list, "t", "c2")).isEqualTo("Incomplete")
        assertThat(member(list, "t", "c3")).isEqualTo("Resolved t.c3")
        val modified = prepare("t.sql" to "CREATE TYPE t AS OBJECT (c1 VARCHAR2(20));", "alter.sql" to "ALTER TYPE t MODIFY ATTRIBUTE c1 VARCHAR2(40) CASCADE;")
        assertThat(member(modified, "t", "c1")).isEqualTo("Resolved t.c1")
        val chain = prepare(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (a1 NUMBER, a2 NUMBER) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (b1 DATE);",
            "alter.sql" to "ALTER TYPE sup DROP ATTRIBUTE a2 CASCADE;"
        )
        assertThat(member(chain, "sub", "a2")).isEqualTo("Incomplete")
        assertThat(member(chain, "sub", "a1")).isEqualTo("Resolved sup.a1")
    }

    @Test
    fun aDropAmongSeveralAttributeClausesIsSeen() {
        val create = "CREATE TYPE t AS OBJECT (c1 NUMBER, c2 NUMBER, c3 NUMBER) NOT FINAL;"
        for (alter in listOf(
            "ALTER TYPE t ADD ATTRIBUTE (c4 NUMBER), DROP ATTRIBUTE c2 CASCADE;",
            // Oracle rejects the form without the comma; the scan treats it the same.
            "ALTER TYPE t ADD ATTRIBUTE (c4 NUMBER) DROP ATTRIBUTE c2 CASCADE;",
            "ALTER TYPE t DROP ATTRIBUTE c2, ADD ATTRIBUTE (c4 NUMBER) CASCADE;"
        )) {
            val context = prepare("t.sql" to create, "alter.sql" to alter)
            assertThat(member(context, "t", "c2")).describedAs(alter).isEqualTo("Incomplete")
            assertThat(member(context, "t", "c1")).describedAs(alter).isEqualTo("Resolved t.c1")
            assertThat(member(context, "t", "c3")).describedAs(alter).isEqualTo("Resolved t.c3")
        }
    }

    @Test
    fun anAlterTypeWithoutSemicolonEndsAtTheSlash() {
        val context = prepare(
            "t.sql" to "CREATE TYPE t AS OBJECT (c1 NUMBER, c2 NUMBER);",
            "alter.sql" to "alter type t add attribute (c3 number)\n/\nalter type other drop attribute c2;\n"
        )
        assertThat(member(context, "t", "c2")).isEqualTo("Resolved t.c2")
    }

    @Test
    fun typesEndedBySlashesAreAllIndexedAndTheirSubtypesInherit() {
        val context = prepare(
            "element.sql" to "create or replace type path_segment as object (\n  indx number(32),\n  name varchar2(4000)\n)\n/\n" +
                "create or replace type path as table of path_segment\n/\n" +
                "create or replace type element force as object\n(\n  typeval number(1),\n  mapname varchar2(4000),\n" +
                "  member function is_object return boolean\n) not final\n/\n" +
                "create or replace type element_array as table of element\n/\n",
            "string.sql" to "create or replace type string force under element (\n  str varchar2(32767)\n)\n/\n"
        )
        val index = (context.state as ProjectAnalysisContext.State.Prepared).result.index
        assertThat(index.declarations.filterIsInstance<StandaloneTypeDeclaration>().map { it.name.toString() })
            .containsExactlyInAnyOrder("path_segment", "path", "element", "element_array", "string")
        assertThat(member(context, "string", "typeval")).isEqualTo("Resolved element.typeval")
        assertThat(member(context, "string", "str")).isEqualTo("Resolved string.str")
        assertThat(member(context, "string", "nothing")).isEqualTo("NotFound")
    }

    @Test
    fun quotedNamesKeepTheirCase() {
        val context = prepare("t.sql" to "CREATE TYPE q AS OBJECT (Mixed NUMBER, \"Quoted\" NUMBER);")
        assertThat(member(context, "q", "MIXED")).isEqualTo("Resolved q.Mixed")
        assertThat(member(context, "q", "\"Quoted\"")).isEqualTo("Resolved q.\"Quoted\"")
        assertThat(member(context, "q", "quoted")).isEqualTo("NotFound")
    }

    @Test
    fun membersOfObjectTypedValuesResolveThroughBothVisitors() {
        val types = "CREATE TYPE addr_t AS OBJECT (city VARCHAR2(30));\n/\n" +
            "CREATE TYPE person_t AS OBJECT (name VARCHAR2(30), home addr_t);\n/\n" +
            "CREATE PACKAGE p AS TYPE pos_r IS RECORD (x NUMBER); END p;\n/\n" +
            "CREATE TYPE shape_t AS OBJECT (pos p.pos_r);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v person_t;\n  s shape_t;\n  n VARCHAR2(30);\nBEGIN\n" +
            "  n := v.name;\n  n := v.home.city;\n  n := s.pos.x;\nEND;\n/\n"
        val members = scan(listOf("types.sql" to types, "use.sql" to use), "use.sql")
        assertThat(members).containsExactly(
            "6 v.name member=Resolved",
            "7 v.home.city path=Completed",
            "8 s.pos.x path=Completed"
        )
    }

    @Test
    fun aCursorAttributeIsNotAMember() {
        val pk = "CREATE PACKAGE pk AS TYPE rc IS REF CURSOR; END pk;\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  c pk.rc;\nBEGIN\n  IF c%NOTFOUND THEN NULL; END IF;\nEND;\n/\n"
        assertThat(scan(listOf("pk.sql" to pk, "use.sql" to use), "use.sql")).isEmpty()
    }

    @Test
    fun theIndexIsTheSameWhateverTheFileOrder() {
        val sources = listOf(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (a1 NUMBER) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (b1 DATE);",
            "alter.sql" to "ALTER TYPE sup ADD ATTRIBUTE (a2 NUMBER) CASCADE;"
        )
        fun index(order: List<Pair<String, String>>, concurrent: Boolean) =
            ProjectIndexPreparation().prepare(order.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent).index
        val first = index(sources, false)
        for (other in listOf(index(sources.reversed(), false), index(sources.reversed(), true))) {
            assertThat(other.declarations).isEqualTo(first.declarations)
            assertThat(other.alteredTypes).isEqualTo(first.alteredTypes)
        }
    }

    private fun scan(sources: List<Pair<String, String>>, observed: String): List<String> {
        val context = prepare(*sources.toTypedArray())
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val semantic = node as SemanticAstNode
                val text = node.tokens.joinToString("") { it.originalValue }
                semantic.projectRecordMemberResolution?.let { rows += "${node.token.line} $text member=${it.javaClass.simpleName}" }
                semantic.projectRecordMemberPathResolution?.let { rows += "${node.token.line} $text path=${it.javaClass.simpleName}" }
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        for ((id, text) in sources) scanner.scanFile(Source(id, text), if (id == observed) listOf(check) else emptyList(), FileId(id))
        return rows
    }

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
