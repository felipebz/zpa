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

class ObjectTypeMethodTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun prepare(vararg sources: Pair<String, String>) =
        ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))

    private fun type(context: ProjectAnalysisContext, name: String): StandaloneTypeDeclaration =
        (context.state as ProjectAnalysisContext.State.Prepared).result.index.declarations.filterIsInstance<StandaloneTypeDeclaration>()
            .single { it.name.last.lookupName == name.uppercase() }

    private fun member(context: ProjectAnalysisContext, typeName: String, member: String): String =
        describe(ProjectRecordMemberResolver(context).resolve(type(context, typeName), OracleIdentifier.fromSource(member)))

    private fun describe(resolution: ProjectRecordMemberResolution): String = when (resolution) {
        is ProjectRecordMemberResolution.Method -> "Method" + (if (resolution.complete) "" else " incomplete") +
            resolution.overloads.joinToString(prefix = " [", postfix = "]") {
                "${it.declaringType.name}.${it.method.name.originalSpelling}/${it.method.parameters.size} ${it.method.methodKind}"
            }
        is ProjectRecordMemberResolution.Resolved -> "Resolved ${resolution.field.name.originalSpelling}"
        else -> resolution.javaClass.simpleName
    }

    @Test
    fun methodsAreExtractedWithKindsModifiersParametersAndReturnTypes() {
        val source = "CREATE TYPE t AS OBJECT (\n" +
            "  a NUMBER,\n" +
            "  MEMBER FUNCTION f(p IN OUT NOCOPY VARCHAR2, q NUMBER DEFAULT 1) RETURN DATE DETERMINISTIC,\n" +
            "  MEMBER PROCEDURE p(SELF IN OUT t),\n" +
            "  STATIC FUNCTION s RETURN t,\n" +
            "  CONSTRUCTOR FUNCTION t(a NUMBER) RETURN SELF AS RESULT,\n" +
            "  MAP MEMBER FUNCTION m RETURN NUMBER,\n" +
            "  NOT INSTANTIABLE NOT FINAL MEMBER FUNCTION abs RETURN NUMBER,\n" +
            "  PRAGMA RESTRICT_REFERENCES(f, WNDS)\n" +
            ") NOT FINAL NOT INSTANTIABLE;"
        val type = extractor.extract(FileId("t.sql"), source).filterIsInstance<StandaloneTypeDeclaration>().single()
        assertThat(type.methodsComplete).isTrue()
        assertThat(type.methods.map { "${it.methodKind} ${it.name.originalSpelling}" })
            .containsExactly("MEMBER f", "MEMBER p", "STATIC s", "CONSTRUCTOR t", "MAP m", "MEMBER abs")
        val f = type.methods[0]
        assertThat(f.parameters.map { "${it.ordinal} ${it.name.originalSpelling} ${it.mode} ${it.nocopy} ${it.typeRef.name} ${it.defaultPresent}" })
            .containsExactly("1 p IN_OUT true VARCHAR2 false", "2 q IN false NUMBER true")
        assertThat(f.returnType!!.name.toString()).isEqualTo("DATE")
        assertThat(f.sourceRange.startLine).isEqualTo(3)
        val p = type.methods[1]
        assertThat(p.parameters).isEmpty()
        assertThat(p.selfParameter!!.mode).isEqualTo(ParameterMode.IN_OUT)
        assertThat(p.isFunction).isFalse()
        val constructor = type.methods[3]
        assertThat(constructor.returnsSelf).isTrue()
        assertThat(constructor.returnType).isNull()
        val abstract = type.methods[5]
        assertThat(abstract.instantiable).isFalse()
        assertThat(abstract.final).isFalse()
    }

    @Test
    fun overridesReplaceAndNewOverloadsExtendTheInheritedSet() {
        val context = prepare(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (x NUMBER, MEMBER FUNCTION f RETURN NUMBER, MEMBER FUNCTION g RETURN NUMBER, " +
                "STATIC FUNCTION s RETURN NUMBER) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (OVERRIDING MEMBER FUNCTION f RETURN NUMBER, MEMBER FUNCTION f(p NUMBER) RETURN NUMBER);"
        )
        assertThat(member(context, "sub", "f")).isEqualTo("Method [sub.f/0 MEMBER, sub.f/1 MEMBER]")
        assertThat(member(context, "sub", "g")).isEqualTo("Method [sup.g/0 MEMBER]")
        // A static method stays in the set with its kind.
        assertThat(member(context, "sub", "s")).isEqualTo("Method [sup.s/0 STATIC]")
        assertThat(member(context, "sub", "x")).isEqualTo("Resolved x")
        assertThat(member(context, "sub", "nothing")).isEqualTo("NotFound")
    }

    @Test
    fun theSetIsIncompleteWhenTheChainMayMissMethods() {
        val orphan = prepare("sub.sql" to "CREATE TYPE sub UNDER missing_t (MEMBER FUNCTION f RETURN NUMBER);")
        assertThat(member(orphan, "sub", "f")).isEqualTo("Method incomplete [sub.f/0 MEMBER]")
        assertThat(member(orphan, "sub", "nothing")).isEqualTo("Incomplete")
        for (alter in listOf("ALTER TYPE t ADD MEMBER FUNCTION h RETURN NUMBER CASCADE;", "ALTER TYPE t DROP MEMBER FUNCTION f RETURN NUMBER CASCADE;",
            "ALTER TYPE t ADD CONSTRUCTOR FUNCTION t(p VARCHAR2) RETURN SELF AS RESULT CASCADE;")) {
            val altered = prepare("t.sql" to "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION f RETURN NUMBER);", "alter.sql" to alter)
            assertThat(member(altered, "t", "f")).describedAs(alter).isEqualTo("Method incomplete [t.f/0 MEMBER]")
            assertThat(member(altered, "t", "nothing")).describedAs(alter).isEqualTo("Incomplete")
            assertThat(member(altered, "t", "a")).describedAs(alter).isEqualTo("Resolved a")
        }
        val unparsed = prepare("t.sql" to "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION f RETURN, MEMBER FUNCTION g RETURN NUMBER);")
        assertThat(type(unparsed, "t").methodsComplete).isFalse()
        assertThat(member(unparsed, "t", "g")).isEqualTo("Method incomplete [t.g/0 MEMBER]")
    }

    @Test
    fun alterTypeIfExistsAndReplaceMakeTheListsIncomplete() {
        val create = "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION g RETURN NUMBER);\n/\n"
        for (alter in listOf(
            "ALTER TYPE IF EXISTS t ADD MEMBER FUNCTION h RETURN NUMBER CASCADE;\n/\n",
            "ALTER TYPE IF EXISTS t ADD ATTRIBUTE (h NUMBER) CASCADE;\n/\n",
            "ALTER TYPE t REPLACE AS OBJECT (a NUMBER, MEMBER FUNCTION g RETURN NUMBER, MEMBER FUNCTION h RETURN NUMBER);\n/\n",
            "ALTER TYPE t REPLACE AUTHID DEFINER AS OBJECT (a NUMBER, h NUMBER, MEMBER FUNCTION g RETURN NUMBER);\n/\n"
        )) {
            val context = prepare("t.sql" to create + alter)
            assertThat(member(context, "t", "h")).describedAs(alter).isEqualTo("Incomplete")
            assertThat(member(context, "t", "g")).describedAs(alter).isEqualTo("Method incomplete [t.g/0 MEMBER]")
        }
    }

    @Test
    fun aSubtypeMethodWithOtherParameterNamesDoesNotOverride() {
        // Oracle rejects OVERRIDING g(q) over g(p) (PLS-00635): parameter names are part of the signature.
        val context = prepare(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (x NUMBER, MEMBER FUNCTION g(p VARCHAR2) RETURN VARCHAR2) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (MEMBER FUNCTION g(q VARCHAR2) RETURN VARCHAR2);"
        )
        assertThat(member(context, "sub", "g")).isEqualTo("Method [sub.g/1 MEMBER, sup.g/1 MEMBER]")
    }

    @Test
    fun methodMembersThroughSelfAValueAndAnExplicitSelf() {
        val spec = "CREATE TYPE t AS OBJECT (a NUMBER, MEMBER FUNCTION f RETURN NUMBER, MEMBER PROCEDURE p(SELF IN OUT t));\n/\n"
        val body = "CREATE TYPE BODY t AS\n" +
            "  MEMBER FUNCTION f RETURN NUMBER IS v t; BEGIN RETURN self.f + v.f; END;\n" +
            "  MEMBER PROCEDURE p(SELF IN OUT t) IS n NUMBER; BEGIN n := self.f; END;\n" +
            "END;\n/\n"
        assertThat(scan("spec.sql" to spec, "body.sql" to body))
            .containsExactly("2 self.f Method [t.f/0 MEMBER]", "2 v.f Method [t.f/0 MEMBER]", "3 self.f Method [t.f/0 MEMBER]")
    }

    @Test
    fun theIndexIsTheSameWhateverTheFileOrder() {
        val sources = listOf(
            "sup.sql" to "CREATE TYPE sup AS OBJECT (x NUMBER, MEMBER FUNCTION f(p NUMBER) RETURN NUMBER) NOT FINAL;",
            "sub.sql" to "CREATE TYPE sub UNDER sup (OVERRIDING MEMBER FUNCTION f(p NUMBER) RETURN NUMBER, STATIC PROCEDURE s);",
            "alter.sql" to "ALTER TYPE sup ADD MEMBER PROCEDURE q CASCADE;"
        )
        fun index(order: List<Pair<String, String>>, concurrent: Boolean) =
            ProjectIndexPreparation().prepare(order.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent).index
        val first = index(sources, false)
        for (other in listOf(index(sources.reversed(), false), index(sources.reversed(), true))) {
            assertThat(other.declarations).isEqualTo(first.declarations)
            assertThat(other.alteredTypes).isEqualTo(first.alteredTypes)
        }
    }

    private fun scan(vararg sources: Pair<String, String>): List<String> {
        val context = prepare(*sources)
        val rows = ArrayList<String>()
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val resolution = (node as SemanticAstNode).projectRecordMemberResolution ?: return
                rows += "${node.token.line} ${node.tokens.joinToString("") { it.originalValue }} ${describe(resolution)}"
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        val observed = sources.last().first
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
