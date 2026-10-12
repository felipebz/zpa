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

/** Regression coverage for all-or-nothing extraction of callable formal signatures. */
class ProjectCallableSignatureExtractionTest {
    private val extractor = ProjectDeclarationExtractor()

    private fun extract(file: String, source: String) = extractor.extract(FileId(file), source)


    @Test
    fun packageSpecificationRetainsCompleteSignaturesAndRecoversAfterRejectedHeaders() {
        val source = """
            CREATE PACKAGE sig_pkg AS
              PROCEDURE broken_first(a NUMBER, b IN OUT);
              SUBTYPE "Mode" IS NUMBER;
              SUBTYPE "Date" IS NUMBER;
              SUBTYPE "ReturnType" IS NUMBER;
              PROCEDURE valid_after_first(x IN NUMBER, y OUT NOCOPY "Mode", z VARCHAR2 DEFAULT 'x');
              PROCEDURE broken_middle(a NUMBER, b NUMBER EXTRA, c NUMBER);
              FUNCTION valid_function(p IN NUMBER DEFAULT 1, q OUT NOCOPY "Date") RETURN "ReturnType";
              PROCEDURE broken_last(a NUMBER, b NUMBER, c IN OUT);
            END sig_pkg;
        """.trimIndent()
        val declarations = extract("sig_spec.sql", source)
        val pkg = declarations.filterIsInstance<PackageDeclaration>().single()
        assertThat(pkg.callableCoverage.omittedNames).containsExactlyInAnyOrder("BROKEN_FIRST", "BROKEN_MIDDLE", "BROKEN_LAST")
        assertThat(pkg.callableCoverage.unknown).isFalse()
        val callables = declarations.filterIsInstance<PackageSubprogramDeclaration>()
        assertThat(callables.map { it.name.lookupName }).containsExactly("VALID_AFTER_FIRST", "VALID_FUNCTION")
        val procedure = callables[0] as PackageProcedureDeclaration
        assertThat(procedure.sourceRange.startLine).isEqualTo(6)
        assertThat(procedure.parameters.map { it.name.lookupName }).containsExactly("X", "Y", "Z")
        assertThat(procedure.parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(procedure.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN)
        assertThat(procedure.parameters.map { it.nocopy }).containsExactly(false, true, false)
        assertThat(procedure.parameters.map { it.defaultPresent }).containsExactly(false, false, true)
        assertThat(procedure.parameters.map { it.typeRef.name.toString() }).containsExactly("NUMBER", "\"Mode\"", "VARCHAR2")
        val function = callables[1] as PackageFunctionDeclaration
        assertThat(function.parameters.map { it.name.lookupName }).containsExactly("P", "Q")
        assertThat(function.parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(function.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT)
        assertThat(function.parameters.map { it.nocopy }).containsExactly(false, true)
        assertThat(function.parameters.map { it.defaultPresent }).containsExactly(true, false)
        assertThat(function.parameters.map { it.typeRef.name.toString() }).containsExactly("NUMBER", "\"Date\"")
        assertThat(function.returnType.name.toString()).isEqualTo("\"ReturnType\"")
    }
    @Test
    fun packageBodyRetainsValidSignaturesAfterRejectedHeader() {
        val source = """
            CREATE PACKAGE BODY body_pkg AS
              PROCEDURE broken(a NUMBER, b IN OUT) IS BEGIN NULL; END;
              PROCEDURE later(x IN NUMBER, y OUT NOCOPY "Mode") IS BEGIN NULL; END;
              FUNCTION good(p IN "Date") RETURN "ReturnType" IS BEGIN RETURN p; END;
            END body_pkg;
        """.trimIndent()
        val facts = extract("body.sql", source)
        assertThat(facts.filterIsInstance<PackageSubprogramDeclaration>().map { it.name.lookupName }).containsExactly("LATER", "GOOD")
        assertThat(facts.filterIsInstance<PackageProcedureDeclaration>().single().parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(facts.filterIsInstance<PackageFunctionDeclaration>().single().returnType.name.toString()).isEqualTo("\"ReturnType\"")
    }

    @Test
    fun standaloneProcedureAndFunctionRetainCompleteFormalFacts() {
        val procedureSource =
            "CREATE PROCEDURE standalone_p(a IN NUMBER, b OUT NOCOPY \"Mode\", c VARCHAR2 DEFAULT 'v') IS BEGIN NULL; END;"
        val functionSource =
            "CREATE FUNCTION standalone_f(x IN \"Date\", y IN NUMBER := 1) RETURN \"ReturnType\" IS BEGIN RETURN x; END;"
        val procedure = extract("standalone_procedure.sql", procedureSource).filterIsInstance<StandaloneProcedureDeclaration>().single()
        assertThat(procedure.parameters.map { it.ordinal }).containsExactly(1, 2, 3)
        assertThat(procedure.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.OUT, ParameterMode.IN)
        assertThat(procedure.parameters.map { it.nocopy }).containsExactly(false, true, false)
        assertThat(procedure.parameters.map { it.defaultPresent }).containsExactly(false, false, true)
        assertThat(procedure.parameters.map { it.typeRef.name.toString() }).containsExactly("NUMBER", "\"Mode\"", "VARCHAR2")
        val function = extract("standalone_function.sql", functionSource).filterIsInstance<StandaloneFunctionDeclaration>().single()
        assertThat(function.parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(function.parameters.map { it.mode }).containsExactly(ParameterMode.IN, ParameterMode.IN)
        assertThat(function.parameters.map { it.defaultPresent }).containsExactly(false, true)
        assertThat(function.parameters.map { it.typeRef.name.toString() }).containsExactly("\"Date\"", "NUMBER")
        assertThat(function.returnType.name.toString()).isEqualTo("\"ReturnType\"")
    }

    @Test
    fun multiwordTypesAndValidModesDefaultsDoNotResolveZeroActualCall() {
        val source = "CREATE PACKAGE modes_pkg AS PROCEDURE m(a IN OUT NOCOPY TIMESTAMP WITH TIME ZONE, " +
            "b VARCHAR2(20 CHAR) DEFAULT 'x'); END modes_pkg;"
        val declarations = extract("modes.sql", source)
        val declaration = declarations.filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(declaration.parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(declaration.parameters.map { it.mode }).containsExactly(ParameterMode.IN_OUT, ParameterMode.IN)
        assertThat(declaration.parameters.map { it.nocopy }).containsExactly(true, false)
        assertThat(declaration.parameters.map { it.defaultPresent }).containsExactly(false, true)
        assertThat(declaration.parameters.map { it.typeRef.name.toString() }).containsExactly("TIMESTAMP", "VARCHAR2")
        assertThat(declaration.parameters[0].typeRef.headerIdentityKey()).contains("WITH").contains("ZONE")
        assertThat(declaration.parameters[1].typeRef.headerIdentityKey()).contains("CHAR")
    }
    @Test
    fun quotedKeywordTypeNamesRemainDatatypesAndRequiredArgumentsStayRequired() {
        val source = """
            CREATE PACKAGE q_words AS
              SUBTYPE "IN" IS NUMBER;
              SUBTYPE "OUT" IS NUMBER;
              SUBTYPE "NOCOPY" IS NUMBER;
              SUBTYPE "DEFAULT" IS NUMBER;
              SUBTYPE "REF" IS NUMBER;
              PROCEDURE run(a "IN", b "OUT", c "NOCOPY", d "DEFAULT", e "REF");
            END;
        """.trimIndent()
        val fact = extract("q_words.sql", source).filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(fact.parameters.map { it.name.lookupName }).containsExactly("A", "B", "C", "D", "E")
        assertThat(fact.parameters.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5)
        assertThat(fact.parameters.map { it.mode }).containsExactly(
            ParameterMode.IN, ParameterMode.IN, ParameterMode.IN, ParameterMode.IN, ParameterMode.IN
        )
        assertThat(fact.parameters.map { it.nocopy }).containsExactly(false, false, false, false, false)
        assertThat(fact.parameters.map { it.defaultPresent }).containsExactly(false, false, false, false, false)
        assertThat(fact.parameters.map { it.typeRef.name.toString() }).containsExactly(
            "\"IN\"", "\"OUT\"", "\"NOCOPY\"", "\"DEFAULT\"", "\"REF\""
        )
        assertThat(fact.parameters.map { it.typeRef }).allMatch { it is NamedTypeRef }

        val prepared = ProjectIndexPreparation(extractor).prepare(listOf(ProjectSource(FileId("q_words.sql")) { source }), false)
        val result = ProjectCallBinder(ProjectAnalysisContext.prepared(prepared)).resolve(
            ProjectCallTestFixtures.member("q_words"),
            ProjectCallKind.PROCEDURE,
            ProjectCallArguments.none()
        )
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE)
    }

    @Test
    fun primaryQuotedInTypeRestoresFormalBindingAndDefaultSemantics() {
        val source = ProjectCallTestFixtures.packageSource(
            "SUBTYPE \"IN\" IS NUMBER; PROCEDURE run(a \"IN\", b NUMBER DEFAULT 7);"
        )
        val fact = extract("primary.sql", source).filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(fact.parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(fact.parameters.map { it.name.lookupName }).containsExactly("A", "B")
        assertThat(fact.parameters.map { it.typeRef.name.toString() }).containsExactly("\"IN\"", "NUMBER")
        assertThat(fact.parameters.map { it.defaultPresent }).containsExactly(false, true)

        val context = ProjectCallTestFixtures.context(source)
        val binder = ProjectCallBinder(context)
        val target = ProjectCallTestFixtures.member()
        val empty = binder.resolve(target, ProjectCallKind.PROCEDURE, ProjectCallArguments.none())
        assertThat(empty.status).isEqualTo(ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE)
        assertThat(empty.candidates.single().missingParameters.map { it.ordinal }).containsExactly(1)

        val named = binder.resolve(target, ProjectCallKind.PROCEDURE, ProjectCallTestFixtures.arguments("a"))
        assertThat(named.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(named.selected!!.bindings.single().parameter.ordinal).isEqualTo(1)
        assertThat(named.selected.omittedParameters.map { it.ordinal }).containsExactly(2)
        val positional = binder.resolve(target, ProjectCallKind.PROCEDURE, ProjectCallTestFixtures.arguments(null))
        assertThat(positional.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        assertThat(positional.selected!!.bindings.single().parameter.name.lookupName).isEqualTo("A")
        assertThat(positional.selected.omittedParameters.map { it.ordinal }).containsExactly(2)
    }

    @Test
    fun quotedKeywordAnchorsAreCompleteAndCorrelateAcrossPackageBody() {
        val inSpec = """
            CREATE PACKAGE q_anchor AS
              SUBTYPE "IN" IS NUMBER;
              PROCEDURE run(a "IN"%TYPE);
            END;
        """.trimIndent()
        val inFact = extract("q_anchor.sql", inSpec).filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(inFact.parameters).hasSize(1)
        assertThat(inFact.parameters.single().defaultPresent).isFalse()
        assertThat(inFact.parameters.single().mode).isEqualTo(ParameterMode.IN)
        assertThat(inFact.parameters.single().typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        val inType = inFact.parameters.single().typeRef as AnchoredTypeRef
        assertThat(inType.name.toString()).isEqualTo("\"IN\"")
        assertThat(inType.anchor).isEqualTo(TypeAnchor.TYPE)

        val anchoredSpec = """
            CREATE PACKAGE q_anchorbad AS
              "DEFAULT" NUMBER;
              PROCEDURE run(a q_anchorbad."DEFAULT"%TYPE);
            END;
        """.trimIndent()
        val anchoredBody = """
            CREATE PACKAGE BODY q_anchorbad AS
              PROCEDURE run(a q_anchorbad."DEFAULT"%TYPE) IS BEGIN NULL; END;
            END;
        """.trimIndent()
        val specFact = extract("q_anchorbad.sql", anchoredSpec).filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(specFact.parameters).hasSize(1)
        assertThat(specFact.parameters.single().ordinal).isEqualTo(1)
        assertThat(specFact.parameters.single().mode).isEqualTo(ParameterMode.IN)
        assertThat(specFact.parameters.single().defaultPresent).isFalse()
        val anchored = specFact.parameters.single().typeRef
        assertThat(anchored).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(anchored.name.toString()).isEqualTo("q_anchorbad.\"DEFAULT\"")
        assertThat((anchored as AnchoredTypeRef).anchor).isEqualTo(TypeAnchor.TYPE)
        val bodyFact = extract("q_anchorbad_body.sql", anchoredBody).filterIsInstance<PackageProcedureDeclaration>().single()
        assertThat(bodyFact.parameters.single().typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(bodyFact.parameters.single().typeRef.name).isEqualTo(anchored.name)
        val prepared = ProjectIndexPreparation(extractor).prepare(listOf(
            ProjectSource(FileId("q_anchorbad.sql")) { anchoredSpec },
            ProjectSource(FileId("q_anchorbad_body.sql")) { anchoredBody }
        ), false)
        val indexed = prepared.index.declarations.filterIsInstance<PackageSubprogramDeclaration>()
        assertThat(indexed).hasSize(2)
        assertThat(indexed[0].headerIdentity()).isEqualTo(indexed[1].headerIdentity())
        val result = ProjectCallBinder(ProjectAnalysisContext.prepared(prepared)).resolve(
            ProjectCallTestFixtures.member("q_anchorbad"),
            ProjectCallKind.PROCEDURE,
            ProjectCallArguments.none()
        )
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.NO_APPLICABLE_CANDIDATE)
    }

    @Test
    fun quotedKeywordReferenceAnchorIsNotMistakenForMode() {
        val source = """
            CREATE PACKAGE q_anchor AS
              SUBTYPE "IN" IS NUMBER;
              FUNCTION f(a "IN"%TYPE) RETURN NUMBER;
            END;
        """.trimIndent()
        val fact = extract("q_anchor_function.sql", source).filterIsInstance<PackageFunctionDeclaration>().single()
        assertThat(fact.parameters).hasSize(1)
        assertThat(fact.parameters.single().mode).isEqualTo(ParameterMode.IN)
        assertThat(fact.parameters.single().defaultPresent).isFalse()
        assertThat(fact.parameters.single().typeRef).isInstanceOf(AnchoredTypeRef::class.java)
        assertThat(fact.returnType.name.toString()).isEqualTo("NUMBER")
    }

    @Test
    fun rejectedSpecificationFormalAddsCoverageWithoutHidingLegacyDuplicateTarget() {
        val context = ProjectCallTestFixtures.context(listOf(
            "retained.sql" to ProjectCallTestFixtures.packageSource("PROCEDURE run(a NUMBER);"),
            "rejected.sql" to ProjectCallTestFixtures.packageSource("PROCEDURE run(a NUMBER EXTRA);")
        ))
        val packages = ProjectCallTestFixtures.index(context).declarations.filterIsInstance<PackageDeclaration>()
        assertThat(packages).hasSize(2)
        assertThat(packages.flatMap { it.callableCoverage.omittedNames }).containsExactly("RUN")

        val owner = ProjectCallTestFixtures.name("p")
        val callable = ProjectCallTestFixtures.identifier("run")
        val legacy = ProjectPackageProcedureResolver(context).resolve(owner, callable)
        assertThat(legacy).isEqualTo(ProjectPackageProcedureResolution.AmbiguousTarget)
        val structural = ProjectCallBinder(context).resolve(
            ProjectCallTestFixtures.member(),
            ProjectCallKind.PROCEDURE,
            ProjectCallArguments.none()
        )
        assertThat(structural.status).isEqualTo(ProjectCallResolution.Status.AMBIGUOUS_TARGET)
    }

}
