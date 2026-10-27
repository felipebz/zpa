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
package com.felipebz.zpa.symbols

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.squid.SemanticAstNode
import com.felipebz.zpa.project.*
import com.felipebz.zpa.squid.AstScanner
import com.felipebz.zpa.symbols.local.LocalCallResolution
import com.felipebz.zpa.symbols.local.LocalSemanticFacts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ObjectMethodCallTest {
    private val spec = "CREATE TYPE t AS OBJECT (\n" +
        "  a NUMBER,\n" +
        "  MEMBER FUNCTION one RETURN NUMBER,\n" +
        "  MEMBER FUNCTION two(p NUMBER) RETURN NUMBER,\n" +
        "  MEMBER FUNCTION two(p NUMBER, q NUMBER) RETURN NUMBER,\n" +
        "  MEMBER FUNCTION nm(p NUMBER) RETURN NUMBER,\n" +
        "  MEMBER FUNCTION nm(q NUMBER) RETURN NUMBER,\n" +
        "  MEMBER PROCEDURE proc,\n" +
        "  STATIC FUNCTION s RETURN NUMBER\n" +
        ") NOT FINAL;\n/\n"

    private fun body(vararg statements: String) =
        "CREATE PROCEDURE use_it IS\n  v t;\n  n NUMBER;\nBEGIN\n" + statements.joinToString("") { "  $it\n" } + "END;\n/\n"

    @Test
    fun valueCallsBindByArityAndNamedNotation() {
        val rows = scan("spec.sql" to spec, "use.sql" to body(
            "n := v.one();", "n := v.one;", "n := v.two(1);", "n := v.two(1, 2);", "n := v.nm(q => 1);", "n := v.nm(1);"
        ))
        assertThat(rows).containsExactly(
            "5 VALUE FUNCTION RESOLVED t.one/0",
            "6 VALUE FUNCTION RESOLVED t.one/0",
            "7 VALUE FUNCTION RESOLVED t.two/1",
            "8 VALUE FUNCTION RESOLVED t.two/2",
            "9 VALUE FUNCTION RESOLVED t.nm/1",
            // Oracle: PLS-00307, too many declarations match.
            "10 VALUE FUNCTION AMBIGUOUS"
        )
    }

    @Test
    fun overloadsThatDifferByTypeAreLeftToDatatypes() {
        val typed = "CREATE TYPE j AS OBJECT (a NUMBER, MEMBER FUNCTION put(p VARCHAR2) RETURN NUMBER, MEMBER FUNCTION put(p NUMBER) RETURN NUMBER);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v j;\n  n NUMBER;\nBEGIN\n  n := v.put(1);\nEND;\n/\n"
        assertThat(scan("j.sql" to typed, "use.sql" to use)).containsExactly("5 VALUE FUNCTION SEVERAL_APPLICABLE")
    }

    @Test
    fun anOverloadThatFitsInANearerTypeWins() {
        val types = "CREATE TYPE sup AS OBJECT (x NUMBER, MEMBER FUNCTION init(p VARCHAR2) RETURN VARCHAR2) NOT FINAL;\n/\n" +
            "CREATE TYPE sub UNDER sup (MEMBER FUNCTION init(p VARCHAR2, q NUMBER := NULL) RETURN VARCHAR2);\n/\n" +
            "CREATE TYPE one AS OBJECT (x NUMBER, MEMBER FUNCTION init(p VARCHAR2) RETURN VARCHAR2, " +
            "MEMBER FUNCTION init(p VARCHAR2, q NUMBER := NULL) RETURN VARCHAR2);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v sub;\n  w one;\n  s VARCHAR2(10);\nBEGIN\n  s := v.init('a');\n  s := w.init('a');\nEND;\n/\n"
        // Oracle runs the subtype's init; in one type the same two overloads are PLS-00307.
        assertThat(scan("types.sql" to types, "use.sql" to use)).containsExactly("6 VALUE FUNCTION RESOLVED sub.init/2", "7 VALUE FUNCTION AMBIGUOUS")
    }

    @Test
    fun overloadsOfSeveralTypesWithOtherParameterTypesAreLeftToDatatypes() {
        // Oracle runs the supertype's f for v.f(1) and the subtype's for v.f('a').
        val types = "CREATE TYPE sup AS OBJECT (x NUMBER, MEMBER FUNCTION f(p NUMBER) RETURN VARCHAR2) NOT FINAL;\n/\n" +
            "CREATE TYPE sub UNDER sup (MEMBER FUNCTION f(q VARCHAR2) RETURN VARCHAR2);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v sub;\n  s VARCHAR2(10);\nBEGIN\n  s := v.f(1);\n  s := v.f('a');\nEND;\n/\n"
        assertThat(scan("types.sql" to types, "use.sql" to use)).containsExactly("5 VALUE FUNCTION SEVERAL_APPLICABLE", "6 VALUE FUNCTION SEVERAL_APPLICABLE")
    }

    @Test
    fun aUserConstructorWithOtherParameterTypesIsLeftToDatatypes() {
        // Oracle runs the attribute-value constructor for t(1) and the user one for t('a').
        val types = "CREATE TYPE ct AS OBJECT (a NUMBER, CONSTRUCTOR FUNCTION ct(z VARCHAR2) RETURN SELF AS RESULT);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v ct;\nBEGIN\n  v := ct(1);\n  v := ct('a');\n  v := ct(a => 1);\nEND;\n/\n"
        assertThat(scan("types.sql" to types, "use.sql" to use))
            .containsExactly("4 CONSTRUCTOR FUNCTION SEVERAL_APPLICABLE", "5 CONSTRUCTOR FUNCTION SEVERAL_APPLICABLE", "6 CONSTRUCTOR FUNCTION RESOLVED ct(attributes)/1")
    }

    @Test
    fun rejectionsAreReportedAsRejections() {
        val rows = scan("spec.sql" to spec, "use.sql" to body("n := v.s();", "n := t.one();", "n := t.s();", "n := v.proc;", "v.one;"))
        assertThat(rows).containsExactly(
            "5 VALUE FUNCTION STATIC_ON_INSTANCE t.s/0",
            "6 TYPE_NAME FUNCTION MEMBER_ON_TYPE t.one/0",
            "7 TYPE_NAME FUNCTION RESOLVED t.s/0",
            "8 VALUE FUNCTION WRONG_KIND",
            "9 VALUE PROCEDURE WRONG_KIND"
        )
    }

    @Test
    fun selfAndAnExplicitSelfBindWithTheSpecificationElsewhere() {
        val typeBody = "CREATE TYPE BODY t AS\n" +
            "  MEMBER FUNCTION one RETURN NUMBER IS BEGIN RETURN self.two(1); END;\n" +
            "  MEMBER FUNCTION two(p NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION two(p NUMBER, q NUMBER) RETURN NUMBER IS BEGIN RETURN 2; END;\n" +
            "  MEMBER FUNCTION nm(p NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION nm(q NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER PROCEDURE proc IS BEGIN NULL; END;\n" +
            "  STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "END;\n/\n"
        assertThat(scan("spec.sql" to spec, "body.sql" to typeBody)).containsExactly("2 SELF FUNCTION RESOLVED t.two/1")
        val explicit = "CREATE TYPE p_t AS OBJECT (a NUMBER, MEMBER PROCEDURE m(SELF IN OUT p_t), MEMBER FUNCTION f(x NUMBER) RETURN NUMBER);\n/\n"
        val explicitBody = "CREATE TYPE BODY p_t AS\n" +
            "  MEMBER PROCEDURE m(SELF IN OUT p_t) IS n NUMBER; BEGIN n := self.f(x => 1); END;\n" +
            "  MEMBER FUNCTION f(x NUMBER) RETURN NUMBER IS BEGIN RETURN x; END;\nEND;\n/\n"
        assertThat(scan("spec.sql" to explicit, "body.sql" to explicitBody)).containsExactly("2 SELF FUNCTION RESOLVED p_t.f/1")
    }

    @Test
    fun constructorsUseTheAttributesInheritedFirstAndPreferAUserConstructor() {
        val types = "CREATE TYPE sup AS OBJECT (a NUMBER, b NUMBER) NOT FINAL;\n/\nCREATE TYPE sub UNDER sup (c VARCHAR2(5));\n/\n" +
            "CREATE TYPE r AS OBJECT (a NUMBER, CONSTRUCTOR FUNCTION r(a NUMBER) RETURN SELF AS RESULT);\n/\n" +
            "CREATE TYPE w AS OBJECT (a NUMBER, CONSTRUCTOR FUNCTION w(z NUMBER) RETURN SELF AS RESULT);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  x sub; y r; z w;\nBEGIN\n" +
            "  x := sub(1, 2, 'x');\n  x := sub(c => 'x', b => 2, a => 1);\n  x := sub(1, 2);\n" +
            "  y := r(a => 1);\n  z := w(1);\n  z := w(a => 1);\nEND;\n/\n"
        assertThat(scan("types.sql" to types, "use.sql" to use)).containsExactly(
            "4 CONSTRUCTOR FUNCTION RESOLVED sub(attributes)/3",
            "5 CONSTRUCTOR FUNCTION RESOLVED sub(attributes)/3",
            "6 CONSTRUCTOR FUNCTION NO_APPLICABLE_CANDIDATE",
            "7 CONSTRUCTOR FUNCTION RESOLVED r.r/1",
            "8 CONSTRUCTOR FUNCTION RESOLVED w.w/1",
            "9 CONSTRUCTOR FUNCTION RESOLVED w(attributes)/1"
        )
    }

    @Test
    fun anIncompleteSetBindsACandidateButProvesNoAbsence() {
        val orphan = "CREATE TYPE o UNDER missing_t (MEMBER FUNCTION f(p NUMBER) RETURN NUMBER);\n/\n"
        val use = "CREATE PROCEDURE use_it IS\n  v o;\n  n NUMBER;\nBEGIN\n  n := v.f(1);\n  n := v.f(1, 2);\n  n := o(1);\nEND;\n/\n"
        assertThat(scan("o.sql" to orphan, "use.sql" to use)).containsExactly(
            "5 VALUE FUNCTION RESOLVED o.f/1 (incomplete)",
            "6 VALUE FUNCTION UNDECIDED (incomplete)",
            "7 CONSTRUCTOR FUNCTION UNDECIDED (incomplete)"
        )
        val altered = "CREATE TYPE t2 AS OBJECT (a NUMBER, MEMBER FUNCTION f RETURN NUMBER);\n/\nALTER TYPE t2 ADD MEMBER FUNCTION f(p NUMBER) RETURN NUMBER CASCADE;\n/\n"
        val use2 = "CREATE PROCEDURE use_it IS\n  v t2;\n  n NUMBER;\nBEGIN\n  n := v.f(1);\nEND;\n/\n"
        assertThat(scan("t2.sql" to altered, "use.sql" to use2)).containsExactly("5 VALUE FUNCTION UNDECIDED (incomplete)")
    }

    @Test
    fun theLocalAnalysisDecidesWhereItDisagrees() {
        // The specification in the same file: the local analysis binds self.two(1) too, and both agree.
        val typeBody = "CREATE TYPE BODY t AS\n" +
            "  MEMBER FUNCTION one RETURN NUMBER IS BEGIN RETURN self.two(1); END;\n" +
            "  MEMBER FUNCTION two(p NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION two(p NUMBER, q NUMBER) RETURN NUMBER IS BEGIN RETURN 2; END;\n" +
            "  MEMBER FUNCTION nm(p NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION nm(q NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER PROCEDURE proc IS BEGIN NULL; END;\n" +
            "  STATIC FUNCTION s RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "END;\n/\n"
        val (rows, _) = scanWithFacts("t.sql" to spec + typeBody)
        assertThat(rows).containsExactly("13 SELF FUNCTION RESOLVED t.two/1")
        // Both overloads of g are read, one with an ACCESSIBLE BY clause: the project and the local analysis both see two
        // applicable overloads for self.g(1), agree that the call is ambiguous, and bind nothing.
        val own = "CREATE TYPE u AS OBJECT (a NUMBER, MEMBER FUNCTION g(p NUMBER) RETURN NUMBER,\n" +
            "  MEMBER FUNCTION g(q NUMBER) RETURN NUMBER ACCESSIBLE BY (FUNCTION x), MEMBER FUNCTION h RETURN NUMBER);\n/\n" +
            "CREATE TYPE BODY u AS\n  MEMBER FUNCTION g(p NUMBER) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION g(q NUMBER) RETURN NUMBER ACCESSIBLE BY (FUNCTION x) IS BEGIN RETURN 2; END;\n" +
            "  MEMBER FUNCTION h RETURN NUMBER IS BEGIN RETURN self.g(1); END;\nEND;\n/\n"
        val (rows2, _) = scanWithFacts("u.sql" to own)
        assertThat(rows2).containsExactly("7 SELF FUNCTION AMBIGUOUS")
    }

    @Test
    fun aLocalProofOfNoCompatibleCandidateDisagreesWithAProjectBinding() {
        val own = "CREATE TYPE d AS OBJECT (a NUMBER, MEMBER FUNCTION g(p DATE) RETURN NUMBER, MEMBER FUNCTION h RETURN NUMBER);\n/\n" +
            "CREATE TYPE BODY d AS\n  MEMBER FUNCTION g(p DATE) RETURN NUMBER IS BEGIN RETURN 1; END;\n" +
            "  MEMBER FUNCTION h RETURN NUMBER IS BEGIN RETURN self.g(TRUE); END;\nEND;\n/\n"
        // The local analysis proves no candidate is compatible with self.g(TRUE): the project's binding is dropped.
        val (rows, facts) = scanWithFacts("d.sql" to own)
        assertThat(facts!!.localCall(5, 50)!!.status).isEqualTo(LocalCallResolution.Status.NO_COMPATIBLE_CANDIDATE)
        assertThat(rows).isEmpty()
    }

    private fun scan(vararg sources: Pair<String, String>) = scanWithFacts(*sources).first

    /** The method-call decorations of the last source, and its local facts. */
    private fun scanWithFacts(vararg sources: Pair<String, String>): Pair<List<String>, LocalSemanticFacts?> {
        val context = ProjectAnalysisContext.prepared(ProjectIndexPreparation().prepare(sources.map { (id, text) -> ProjectSource(FileId(id)) { text } }, concurrent = false))
        val rows = ArrayList<String>()
        var facts: LocalSemanticFacts? = null
        val check = object : PlSqlCheck() {
            init { subscribeTo(PlSqlGrammar.METHOD_CALL, PlSqlGrammar.MEMBER_EXPRESSION) }
            override fun visitNode(node: AstNode) {
                val call = (node as SemanticAstNode).projectMethodCall ?: return
                rows += "${node.token.line} ${call.receiver} ${call.kind} ${call.status}" + (call.selected?.let { " ${describe(it)}" } ?: "") +
                    (if (call.complete) "" else " (incomplete)")
            }
            override fun leaveFile(node: AstNode) {
                facts = this.context.localSemanticFacts
            }
        }
        val scanner = AstScanner(emptyList(), null, true, StandardCharsets.UTF_8, context)
        val observed = sources.last().first
        for ((id, text) in sources) scanner.scanFile(Source(id, text), if (id == observed) listOf(check) else emptyList(), FileId(id))
        return rows to facts
    }

    private fun describe(candidate: ProjectMethodCallCandidate) =
        candidate.method?.let { "${candidate.type.name}.${it.name.originalSpelling}/${candidate.parameters.size}" }
            ?: "${candidate.type.name}(attributes)/${candidate.parameters.size}"

    private class Source(private val id: String, private val text: String) : PlSqlFile {
        override fun contents() = text
        override fun fileName() = id
        override fun path(): Path = Path.of(id)
        override fun type() = PlSqlFile.Type.MAIN
    }
}
