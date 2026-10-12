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

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.zpa.api.PlSqlKeyword
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.extraction.TokenStream
import com.felipebz.zpa.project.extraction.TypeReferences
import com.felipebz.zpa.squid.PlSqlConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Oracle identifier identity: `run`, `RUN` and `"RUN"` are one name; `"run"` and `"Run"` are other names. */
@OptIn(ZpaExperimentalApi::class)
class OracleIdentifierIdentityTest {
    private fun id(s: String) = OracleIdentifier.fromSource(s)
    private fun qn(vararg s: String) = QualifiedName(s.map(::id))
    private fun pn(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun pi(s: String) = ProjectIdentifier.fromSource(s)

    private fun snapshot(vararg files: Pair<String, String>, concurrent: Boolean = false) =
        ProjectSnapshots.prepare(files.map { (f, t) -> ProjectSourceInput.ofText(f, t) }, concurrent)

    @Test
    fun equalityHashAndSpellingFollowTheNormalizedName() {
        val equivalent = listOf("run", "RUN", "Run", "\"RUN\"").map(::id)
        for (a in equivalent) for (b in equivalent) {
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
        }
        assertThat(equivalent.toSet()).hasSize(1)
        assertThat(equivalent.map { it.originalSpelling }).containsExactly("run", "RUN", "Run", "\"RUN\"")
        assertThat(equivalent.map { it.quoted }).containsExactly(false, false, false, true)
        assertThat(equivalent.map { it.lookupName }.toSet()).containsExactly("RUN")
        assertThat(id("\"run\"")).isNotEqualTo(id("run")).isNotEqualTo(id("\"Run\"")).isNotEqualTo(id("\"RUN\""))
        assertThat(id("\"Run\"")).isNotEqualTo(id("RUN"))
        assertThat(hashSetOf(id("\"RUN\"")).contains(id("run"))).isTrue()
    }

    @Test
    fun qualifiedNamesAndKeysAgreeAndStayInjective() {
        assertThat(qn("p", "run")).isEqualTo(qn("\"P\"", "\"RUN\"")).isEqualTo(qn("P", "\"RUN\""))
            .hasSameHashCodeAs(qn("\"P\"", "run"))
        assertThat(qn("p", "run").lookupKey()).isEqualTo(qn("\"P\"", "\"RUN\"").lookupKey()).isEqualTo(qn("P", "\"RUN\"").lookupKey())
        assertThat(qn("p", "run")).isNotEqualTo(qn("p", "\"run\"")).isNotEqualTo(qn("p", "\"Run\""))
        assertThat(qn("p", "run").lookupKey()).isNotEqualTo(qn("p", "\"run\"").lookupKey()).isNotEqualTo(qn("p", "\"Run\"").lookupKey())
        // dots, digits and length prefixes inside a quoted segment cannot imitate a segment boundary
        val keys = listOf(qn("\"a.b\""), qn("a", "b"), qn("\"1:a\"", "b"), qn("\"a\"", "\"1:b\""), qn("\"3:a.1:b\""), qn("\"a.1:b\""), qn("\"a\"", "\"b\""))
        val byKey = keys.groupBy { it.lookupKey() }.values.map { group -> group.map { it.toString() } }
        for (group in byKey) for (a in group) for (b in group) {
            assertThat(keys.first { it.toString() == a }).isEqualTo(keys.first { it.toString() == b })
        }
        assertThat(qn("\"a.b\"").lookupKey()).isNotEqualTo(qn("a", "b").lookupKey())
    }

    @Test
    fun publicIdentifiersAndNamesCompareLikeOracle() {
        assertThat(pi("run")).isEqualTo(pi("\"RUN\"")).hasSameHashCodeAs(pi("RUN"))
        assertThat(pi("\"RUN\"").isQuoted).isTrue()
        assertThat(pi("\"RUN\"").originalSpelling).isEqualTo("\"RUN\"")
        assertThat(pi("\"run\"")).isNotEqualTo(pi("run"))
        assertThat(pn("p", "run")).isEqualTo(pn("\"P\"", "\"RUN\"")).hasSameHashCodeAs(pn("P", "run"))
        assertThat(pn("p", "run")).isNotEqualTo(pn("p", "\"Run\""))
        assertThat(pn("\"a.b\"")).isNotEqualTo(pn("a", "b"))
    }

    @Test
    fun equivalentSpellingsAreSeparateOccurrencesFoundByEitherQuery() {
        val s = snapshot("p.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); PROCEDURE \"RUN\"(a VARCHAR2); PROCEDURE \"run\"(a DATE); END p;")
        val viaRun = s.findPackageSubprograms(pn("p"), pi("run")).declarations
        val viaQuoted = s.findPackageSubprograms(pn("\"P\""), pi("\"RUN\"")).declarations
        assertThat(viaRun).hasSize(2)
        assertThat(viaRun.map { it.id }).isEqualTo(viaQuoted.map { it.id }).doesNotHaveDuplicates()
        assertThat(viaRun.map { it.localName.originalSpelling }).containsExactly("run", "\"RUN\"")
        assertThat(s.findDeclarations(pn("p", "\"RUN\"")).declarations).hasSize(2)
        assertThat(s.findPackageSubprograms(pn("p"), pi("\"run\"")).declarations).hasSize(1)
        assertThat(s.declarations.count { it.kind == com.felipebz.zpa.api.project.ProjectDeclarationKind.PACKAGE_PROCEDURE }).isEqualTo(3)
    }

    @Test
    fun specificationAndBodyCorrelateAcrossSpelling() {
        val s = snapshot(
            "s.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER, b \"MY_T\"); END p;",
            "b.sql" to "CREATE PACKAGE BODY p AS PROCEDURE \"RUN\"(\"A\" NUMBER, b my_t) IS BEGIN NULL; END; END p;"
        )
        val body = s.declarations.single { it.role == ProjectDeclarationRole.BODY }
        val c = s.correlateSpecification(body.id)
        assertThat(c.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        // a genuinely different quoted name does not correlate
        val other = snapshot(
            "s.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); END p;",
            "b.sql" to "CREATE PACKAGE BODY p AS PROCEDURE \"run\"(a NUMBER) IS BEGIN NULL; END; END p;"
        )
        assertThat(other.correlateSpecification(other.declarations.single { it.role == ProjectDeclarationRole.BODY }.id).status)
            .isEqualTo(ProjectSpecificationCorrelation.Status.NOT_FOUND)
    }

    @Test
    fun typesValuesFieldsAndAnchorsResolveAcrossSpelling() {
        val s = snapshot(
            "p.sql" to """
                CREATE PACKAGE p AS
                  TYPE "REC_T" IS RECORD (fld NUMBER);
                  SUBTYPE "SUB_T" IS NUMBER;
                  "G" NUMBER;
                  v_sub sub_t;
                  v_anchor g%TYPE;
                  v_rec rec_t;
                END p;
            """.trimIndent()
        )
        assertThat(s.resolveNamedType(pn("rec_t"), pn("\"P\"")).status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(s.resolveNamedType(pn("\"p\"", "rec_t")).status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(s.resolvePackageValue(pn("g"), pn("p")).status).isEqualTo(ProjectValueResolution.Status.RESOLVED)
        assertThat(s.resolvePackageValue(pn("\"g\""), pn("p")).status).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT)
        val sub = s.findDeclarations(pn("p", "v_sub")).declarations.single() as ProjectPackageValueView
        assertThat(s.describeType(sub.typeRef, sub.owner).status).isEqualTo(ProjectTypeDescription.Status.BUILT_IN)
        val anchor = s.findDeclarations(pn("p", "v_anchor")).declarations.single() as ProjectPackageValueView
        val d = s.describeType(anchor.typeRef, anchor.owner)
        assertThat(d.status).isEqualTo(ProjectTypeDescription.Status.BUILT_IN)
        assertThat(d.values.map { it.localName.originalSpelling }).containsExactly("\"G\"")
        val rec = s.resolveNamedType(pn("rec_t"), pn("p")).declaration.get() as ProjectPackageTypeView
        assertThat(rec.recordFields.single().name).isEqualTo(pi("\"FLD\""))
    }

    @Test
    fun recordMemberPathsMatchByNormalizedName() {
        val record = ProjectDeclarationExtractor().extract(FileId("r.sql"), "CREATE PACKAGE p AS TYPE t IS RECORD (id NUMBER, \"Id\" NUMBER); END p;")
            .filterIsInstance<PackageTypeDeclaration>().single()
        val resolver = ProjectRecordMemberResolver()
        assertThat(resolver.resolve(record, id("\"ID\""))).isInstanceOf(ProjectRecordMemberResolution.Resolved::class.java)
        assertThat(resolver.resolve(record, id("\"Id\""))).isInstanceOf(ProjectRecordMemberResolution.Resolved::class.java)
    }

    @Test
    fun coverageNamesAreNormalizedAcrossQuoting() {
        val facts = ProjectDeclarationExtractor().extract(
            FileId("c.sql"),
            "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); FUNCTION \"RUN\"(t TABLE) RETURN TABLE PIPELINED ROW POLYMORPHIC USING p; END p;"
        )
        val coverage = facts.filterIsInstance<PackageDeclaration>().single().callableCoverage
        assertThat(coverage.omittedNames).containsExactly("RUN")
        assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().single().name).isEqualTo(id("\"RUN\""))
    }

    @Test
    fun quotedBuiltInSpellingsStayUnclassified() {
        assertThat(BuiltInTypeRefClassifier.classify(qn("\"NUMBER\""))).isNull()
        assertThat(BuiltInTypeRefClassifier.classify(qn("number"))).isNotNull()
        val unquoted = NamedTypeRef(qn("DATE"), SourceRange(FileId("f"), 1, 0, 1, 4))
        val quoted = NamedTypeRef(qn("\"DATE\""), SourceRange(FileId("f"), 1, 0, 1, 6))
        assertThat(quoted.structuralKey()).isEqualTo(unquoted.structuralKey())
        val ordinary = NamedTypeRef(qn("\"MY_T\""), SourceRange(FileId("f"), 1, 0, 1, 6))
        assertThat(ordinary.structuralKey()).isEqualTo(NamedTypeRef(qn("my_t"), SourceRange(FileId("f"), 1, 0, 1, 4)).structuralKey())
        val s = snapshot("p.sql" to "CREATE PACKAGE p AS SUBTYPE a IS \"NUMBER\"; SUBTYPE b IS NUMBER; END p;")
        assertThat(s.describeType(pn("a"), pn("p")).status).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME)
        assertThat(s.describeType(pn("b"), pn("p")).status).isEqualTo(ProjectTypeDescription.Status.BUILT_IN)
        val facts = ProjectDeclarationExtractor().extract(FileId("o.sql"), "CREATE PACKAGE p AS PROCEDURE f(x DATE); PROCEDURE f(x \"DATE\"); END p;")
            .filterIsInstance<PackageProcedureDeclaration>()
        assertThat(facts[0].overloadIdentity()).isEqualTo(facts[1].overloadIdentity())
        assertThat(facts[0].headerIdentity()).isEqualTo(facts[1].headerIdentity())
    }

    @Test
    fun nonReservedKeywordSpellingsHaveEquivalentCompleteHeaders() {
        val lexer = PlSqlLexer.create(PlSqlConfiguration(java.nio.charset.StandardCharsets.UTF_8))
        val tokens = lexer.lex("NUMBER \"NUMBER\"").filter { it.type != GenericTokenType.EOF }
        assertThat(tokens.map { it.type }).containsExactly(PlSqlKeyword.NUMBER, GenericTokenType.IDENTIFIER)
        assertThat(PlSqlKeyword.NUMBER.isReserved).isFalse()
        val facts = ProjectDeclarationExtractor().extract(
            FileId("headers.sql"),
            """CREATE PACKAGE p AS PROCEDURE f(x NUMBER(10)); PROCEDURE f(x "NUMBER"(10)); PROCEDURE f(x NUMBER(11)); END p;"""
        ).filterIsInstance<PackageProcedureDeclaration>()
        assertThat(facts[0].headerIdentity()).isEqualTo(facts[1].headerIdentity())
        assertThat(facts[0].overloadIdentity()).isEqualTo(facts[1].overloadIdentity())
        assertThat(facts[0].headerIdentity()).isNotEqualTo(facts[2].headerIdentity())
    }

    @Test
    fun reservedKeywordsRetainTheirHeaderSyntaxDistinction() {
        fun reference(text: String): TypeRef {
            val tokens = PlSqlLexer.create(PlSqlConfiguration(java.nio.charset.StandardCharsets.UTF_8)).lex(text).filter { it.type != GenericTokenType.EOF }
            return requireNotNull(TypeReferences(TokenStream(FileId("header.sql"), tokens)).typeReference(0, tokens.size))
        }
        assertThat(PlSqlKeyword.WITH.isReserved).isTrue()
        assertThat(reference("TIMESTAMP WITH TIME ZONE").headerIdentityKey())
            .isNotEqualTo(reference("""TIMESTAMP "WITH" TIME ZONE""").headerIdentityKey())
    }

    @Test
    fun predefinedDatatypeSpellingsCorrelateInBothDirectionsWithOrWithoutShadowing() {
        for (shadow in listOf("", """TYPE "DATE" IS RECORD (v NUMBER);""")) {
            for ((specType, bodyType) in listOf("DATE" to "\"DATE\"", "\"DATE\"" to "DATE")) {
                val s = snapshot(
                    "s.sql" to "CREATE PACKAGE p AS $shadow PROCEDURE f(x $specType); END p;",
                    "b.sql" to "CREATE PACKAGE BODY p AS PROCEDURE f(x $bodyType) IS BEGIN NULL; END; END p;"
                )
                val body = s.declarations.single { it.role == ProjectDeclarationRole.BODY }
                val spec = s.declarations.single { it.kind == com.felipebz.zpa.api.project.ProjectDeclarationKind.PACKAGE_PROCEDURE && it.role == ProjectDeclarationRole.SPECIFICATION }
                val correlation = s.correlateSpecification(body.id)
                assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
                assertThat(correlation.candidates.map { it.id }).containsExactly(spec.id)
            }
        }
    }

    @Test
    fun serialConcurrentAndReversedPreparationAgree() {
        val files = listOf(
            "a.sql" to "CREATE PACKAGE p AS PROCEDURE run(a NUMBER); END p;",
            "b.sql" to "CREATE PACKAGE p AS PROCEDURE \"RUN\"(a NUMBER); END p;",
            "c.sql" to "CREATE PACKAGE BODY \"P\" AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; END \"P\";"
        )
        fun shape(s: ProjectSnapshot) = s.findPackageSubprograms(pn("p"), pi("RUN")).declarations.map { it.id.toString() to it.role }
        val serial = shape(snapshot(*files.toTypedArray()))
        assertThat(serial).hasSize(3)
        assertThat(shape(snapshot(*files.toTypedArray(), concurrent = true))).isEqualTo(serial)
        assertThat(shape(snapshot(*files.reversed().toTypedArray(), concurrent = true))).isEqualTo(serial)
    }
}
