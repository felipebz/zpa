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
package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.PlSqlFile
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.checks.PlSqlCheck
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.project.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.nio.Buffer
import java.util.*
import java.util.stream.BaseStream
import java.lang.reflect.Array as ReflectArray

@OptIn(ZpaExperimentalApi::class)
class ProjectSnapshotRetentionTest {
    @Test
    fun snapshotRetainsOnlyDurableFactsAndPublicMetadata() {
        val sentinel = Any()
        val sourceMarker = "snapshot-retention-comment-720827b1-9ee4-4d06-a15f-53e81c6c2331"
        val source = "-- $sourceMarker\n" + """
            CREATE PACKAGE retained_facts AS
              TYPE row_t IS RECORD (id NUMBER, title VARCHAR2(30));
              FUNCTION lookup(value IN row_t) RETURN NUMBER;
            END retained_facts;
        """.trimIndent()
        val reader = ProjectSourceReader {
            sentinel.hashCode()
            source
        }
        val cause = IllegalArgumentException("cause: $sourceMarker")
        val exception = IllegalStateException("failure: $sourceMarker", cause)
        val failedReader = ProjectSourceReader { throw exception }
        val input = ProjectSourceInput.ofReader("facts.sql", reader)
        val failedInput = ProjectSourceInput.ofReader("failed.sql", failedReader)
        val inputs = arrayListOf(input, failedInput)

        val snapshot = ProjectSnapshots.prepare(inputs, false)

        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        assertThat(snapshot.attemptedFileCount).isEqualTo(2)
        assertThat(snapshot.successfulFileCount).isEqualTo(1)
        assertThat(snapshot.fileIds).containsExactly("facts.sql", "failed.sql")
        assertThat(snapshot.failures).hasSize(1)
        assertThat(snapshot.failures.single().exceptionType).isEqualTo(exception.javaClass.name)

        val roots = linkedMapOf<String, Any?>(
            "snapshot" to snapshot,
            "metadata.preparationState" to snapshot.preparationState,
            "metadata.attemptedFileCount" to snapshot.attemptedFileCount,
            "metadata.successfulFileCount" to snapshot.successfulFileCount,
            "metadata.failures" to snapshot.failures,
            "metadata.fileIds" to snapshot.fileIds
        )
        val reachable = auditGraph(
            roots,
            listOf(sentinel, source, reader, failedReader, input, failedInput, inputs, exception, cause),
            sourceMarker
        )

        // A metadata-only or accidentally empty graph must not make this audit pass.
        assertThat(reachable.filterIsInstance<ProjectDeclaration>()).hasSize(3)
        assertThat(reachable.filterIsInstance<PackageTypeDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<PackageFunctionDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectRecordField>()).hasSize(2)
        assertThat(reachable.filterIsInstance<ProjectParameter>()).hasSize(1)
        assertThat(reachable.filterIsInstance<TypeRef>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<SourceRange>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<QualifiedName>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
    }

    @Test
    fun declarationViewsIdsAndInventoriesRetainOnlyDurableFacts() {
        val sourceMarker = "inventory-retention-comment-5b0c4d4e-8c1f-4f6e-a1de-0d58d9a9bb11"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE inv AS TYPE row_t IS RECORD (id NUMBER); FUNCTION lookup(v IN row_t) RETURN NUMBER; END inv;"
        val reader = ProjectSourceReader { source }
        val failedReader = ProjectSourceReader { throw IllegalStateException("failure: $sourceMarker") }
        val input = ProjectSourceInput.ofReader("inv.sql", reader)
        val failedInput = ProjectSourceInput.ofReader("failed.sql", failedReader)
        val snapshot = ProjectSnapshots.prepare(listOf(input, failedInput), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, failedReader, input, failedInput)

        val all = snapshot.declarations
        val wholeList = all
        val view = all[1]
        val idAlone = view.id
        val perFile = snapshot.declarationsFor("inv.sql")
        val byName = snapshot.findDeclarations(ProjectQualifiedName.of("inv", "lookup"))
        assertThat(byName.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)
        assertThat(byName.declarations).hasSize(1)

        val roots = mapOf<String, Any?>(
            "view" to view, "id" to idAlone, "wholeProject" to wholeList,
            "perFile" to perFile, "byName" to byName
        )
        val reachable = auditGraph(roots, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<ProjectDeclaration>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)

        // An ID alone must not reach the snapshot, index or any retained fact.
        val idReachable = auditGraph(mapOf("id" to idAlone), forbidden, sourceMarker)
        assertThat(idReachable.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(idReachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(idReachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun typeViewsFieldsAndReferencesRetainOnlyDurableFacts() {
        val sourceMarker = "type-view-retention-1f0c9d7a-3a52-4e54-93a8-0d1d3d5e7a10"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE tv AS TYPE r IS RECORD (a NUMBER, b e.c%TYPE, c REF o); SUBTYPE s IS tv2.t; END tv;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("tv.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)

        val type = snapshot.findDeclarations(ProjectQualifiedName.of("tv", "r")).declarations.single()
            as ProjectPackageTypeView
        val subtype = snapshot.findDeclarations(ProjectQualifiedName.of("tv", "s")).declarations.single()
            as ProjectSubtypeView
        val fields = type.recordFields
        val roots = mapOf<String, Any?>(
            "type" to type, "fields" to fields, "field" to fields[1], "typeRefs" to fields.map { it.typeRef },
            "shape" to type.shape, "base" to subtype.baseType, "subtype" to subtype
        )
        val reachable = auditGraph(roots, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<PackageTypeDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectRecordField>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectTypeReference>()).hasSize(4)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()

        val idOnly = auditGraph(mapOf("id" to type.id), forbidden, sourceMarker)
        assertThat(idOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        // Public type-reference values carry only names, ranges and enums.
        val refOnly = auditGraph(mapOf("ref" to fields[1].typeRef), forbidden, sourceMarker)
        assertThat(refOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(refOnly.filterIsInstance<TypeRef>()).isEmpty()
    }

    @Test
    fun collectionElementReferencesRetainOnlyDurableFacts() {
        val sourceMarker = "element-view-retention-4e7a1c90-5b2d-4c1f-9a63-7d0e2f8b1c55"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE ev AS TYPE r IS RECORD (a NUMBER); TYPE rows_t IS TABLE OF r INDEX BY PLS_INTEGER; " +
            "TYPE a_t IS VARRAY(3) OF e.c%TYPE; END ev;\n/\nCREATE TYPE ids AS TABLE OF app.obj_t;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("ev.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)

        val rows = snapshot.findDeclarations(ProjectQualifiedName.of("ev", "rows_t")).declarations.single() as ProjectPackageTypeView
        val array = snapshot.findDeclarations(ProjectQualifiedName.of("ev", "a_t")).declarations.single() as ProjectPackageTypeView
        val ids = snapshot.findDeclarations(ProjectQualifiedName.of("ids")).declarations.single() as ProjectShapedTypeView
        val elements = listOf(rows.elementType.get(), array.elementType.get(), ids.elementType.get())
        val reachable = auditGraph(
            mapOf<String, Any?>("rows" to rows, "array" to array, "ids" to ids, "elements" to elements),
            forbidden, sourceMarker
        )
        assertThat(reachable.filterIsInstance<PackageTypeDeclaration>()).hasSize(2)
        assertThat(reachable.filterIsInstance<StandaloneTypeDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()

        // A public element reference alone reaches no internal fact, TypeRef, snapshot, index or source.
        val referenceOnly = auditGraph(mapOf<String, Any?>("elements" to elements), forbidden, sourceMarker)
        assertThat(referenceOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(referenceOnly.filterIsInstance<TypeRef>()).isEmpty()
        assertThat(referenceOnly.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun callTargetsAndCandidateDiscoveryRetainOnlyDurableFacts() {
        val sourceMarker = "call-discovery-retention-8d2f6b1e-4c7a-4f0e-9b3d-6a1e5c2d7f90"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE x AS PROCEDURE run(a NUMBER); " +
            "FUNCTION run(t NUMBER) RETURN NUMBER AS LANGUAGE JAVA NAME 'A.f(int) return int'; END x;\n/\n" +
            "CREATE PACKAGE BODY x AS PROCEDURE run(a NUMBER) IS BEGIN NULL; END; END x;\n/\n" +
            "CREATE PROCEDURE x.run IS BEGIN NULL; END;"
        val reader = ProjectSourceReader { source }
        val failedReader = ProjectSourceReader { throw IllegalStateException("failure: $sourceMarker") }
        val input = ProjectSourceInput.ofReader("calls.sql", reader)
        val failedInput = ProjectSourceInput.ofReader("failed.sql", failedReader)
        val snapshot = ProjectSnapshots.prepare(listOf(input, failedInput), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, failedReader, input, failedInput)

        val target = ProjectCallTarget.written(ProjectQualifiedName.of("x", "run"))
        val result = snapshot.findCallCandidates(target)
        assertThat(result.status).isEqualTo(ProjectCallCandidates.Status.INCOMPLETE_INDEX)
        assertThat(result.candidates).hasSize(2)

        val reachable = auditGraph(mapOf<String, Any?>("result" to result), forbidden, sourceMarker)
        // The specification member, the standalone and the package specification; never the body.
        assertThat(reachable.filterIsInstance<ProjectDeclaration>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectDeclaration>().map { it.role }).doesNotContain(DeclarationRole.BODY)
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectAnalysisContext>()).isEmpty()
        assertThat(reachable.filterIsInstance<IndexedDeclaration>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectCallCandidates>()).isEmpty()

        // A request value holds only its form and names.
        val targetOnly = auditGraph(mapOf<String, Any?>("target" to target), forbidden, sourceMarker)
        assertThat(targetOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(targetOnly.filterIsInstance<com.felipebz.zpa.project.ProjectCallTarget>()).isEmpty()
    }

    @Test
    fun callResolutionRetainsOnlyRequestValuesAndDurableFacts() {
        val sourceMarker = "call-resolution-retention-3b9e1f42-7c5d-4a8e-b6f0-2d4c8a1e9f73"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE r AS PROCEDURE run(a NUMBER, b NUMBER DEFAULT 1); PROCEDURE run(c NUMBER); END r;\n/\n" +
            "CREATE PACKAGE BODY r AS PROCEDURE run(c NUMBER) IS BEGIN NULL; END; END r;"
        val reader = ProjectSourceReader { source }
        val failedReader = ProjectSourceReader { throw IllegalStateException("failure: $sourceMarker") }
        val input = ProjectSourceInput.ofReader("calls.sql", reader)
        val failedInput = ProjectSourceInput.ofReader("failed.sql", failedReader)
        val target = ProjectCallTarget.packageMember(ProjectQualifiedName.of("r"), ProjectIdentifier.fromSource("run"))
        val arguments = ProjectCallArguments.of(listOf(ProjectCallArgument.named(ProjectIdentifier.fromSource("a"))))
        val forbiddenFor = { snapshot: ProjectSnapshot ->
            val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
                .apply { isAccessible = true }.get(snapshot)
            val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
            listOf(snapshot, resultField, index, source, reader, failedReader, input, failedInput)
        }

        val resolved = ProjectSnapshots.prepare(listOf(input), false)
        val result = resolved.resolveCall(target, ProjectCallKind.PROCEDURE, arguments)
        assertThat(result.status).isEqualTo(ProjectCallResolution.Status.RESOLVED)
        val incomplete = ProjectSnapshots.prepare(listOf(input, failedInput), false)
            .resolveCall(target, ProjectCallKind.PROCEDURE, arguments)
        assertThat(incomplete.status).isEqualTo(ProjectCallResolution.Status.INCOMPLETE_INDEX)

        val reachable = auditGraph(mapOf<String, Any?>("result" to result), forbiddenFor(resolved), sourceMarker)
        // The two specification overloads and the package specification as evidence; never the body.
        assertThat(reachable.filterIsInstance<PackageProcedureDeclaration>()).hasSize(2)
        assertThat(reachable.filterIsInstance<PackageDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectDeclaration>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectDeclaration>().map { it.role }).containsOnly(DeclarationRole.SPECIFICATION)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectAnalysisContext>()).isEmpty()
        assertThat(reachable.filterIsInstance<IndexedDeclaration>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectCallResolution>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectCallCandidate>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectArgumentBinding>()).isEmpty()
        val incompleteReachable = auditGraph(mapOf<String, Any?>("incomplete" to incomplete), listOf(source, reader, failedReader), sourceMarker)
        assertThat(incompleteReachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
        assertThat(incompleteReachable.filterIsInstance<ProjectSnapshot>()).isEmpty()

        // A request holds only identifiers.
        val request = auditGraph(mapOf<String, Any?>("target" to target, "arguments" to arguments), emptyList(), sourceMarker)
        assertThat(request.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(request.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun anchorDescriptionsRetainOnlyDurableFacts() {
        val sourceMarker = "anchor-retention-8a3c5e17-2d94-4f60-b1a8-6e0d9c7f2b35"
        val source = "-- $sourceMarker\nCREATE PACKAGE ar AS g NUMBER; a g%TYPE; b a%TYPE; c ext.col%TYPE; d x%ROWTYPE; e1 e2%TYPE; e2 e1%TYPE; END ar;\n/\nCREATE PACKAGE cy1 AS x cy2.y%TYPE; END cy1;\n/\nCREATE PACKAGE cy2 AS y cy1.x%TYPE; END cy2;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("ar.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)
        fun describe(owner: String, name: String): ProjectTypeDescription {
            val value = snapshot.findDeclarations(ProjectQualifiedName.of(owner, name)).declarations.single() as ProjectPackageValueView
            return snapshot.describeType(value.typeRef, value.owner)
        }
        val descriptions = mapOf(
            "chain" to describe("ar", "b"), "unresolved" to describe("ar", "c"), "stopped" to describe("ar", "d"),
            "declaredLater" to describe("ar", "e1"), "cycle" to describe("cy1", "x")
        )
        assertThat(descriptions.values.map { it.status }).containsExactly(
            ProjectTypeDescription.Status.BUILT_IN, ProjectTypeDescription.Status.UNRESOLVED_VALUE,
            ProjectTypeDescription.Status.ANCHORED_REFERENCE, ProjectTypeDescription.Status.ANCHORED_REFERENCE,
            ProjectTypeDescription.Status.CYCLE
        )
        assertThat(descriptions.getValue("declaredLater").anchorStop).contains(ProjectTypeDescription.AnchorStop.DECLARED_LATER)
        assertThat(descriptions.getValue("cycle").steps).hasSize(2)
        val reachable = auditGraph(descriptions, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<PackageValueDeclaration>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThatThrownBy { (descriptions.getValue("chain").steps as MutableList<ProjectDeclarationView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun writtenAnchorDescriptionsRetainOnlyDurableFactsAndTheRequest() {
        val sourceMarker = "written-anchor-retention-3f7b9c21-5e04-4a8d-9b16-c2d4e8a0f513"
        val source = "-- $sourceMarker\nCREATE PACKAGE wr AS g NUMBER; a g%TYPE; c ext.col%TYPE; END wr;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("wr.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)
        fun written(anchor: ProjectTypeReference.Anchor, vararg name: String) =
            snapshot.describeType(ProjectWrittenAnchor.of(ProjectQualifiedName.of(*name), anchor), null)
        val descriptions = mapOf(
            "followed" to written(ProjectTypeReference.Anchor.TYPE, "wr", "a"),
            "retainedStop" to written(ProjectTypeReference.Anchor.TYPE, "wr", "c"),
            "requestStop" to written(ProjectTypeReference.Anchor.TYPE, "employees", "salary"),
            "row" to written(ProjectTypeReference.Anchor.ROWTYPE, "employees")
        )
        assertThat(descriptions.values.map { it.status }).containsExactly(
            ProjectTypeDescription.Status.BUILT_IN, ProjectTypeDescription.Status.UNRESOLVED_VALUE,
            ProjectTypeDescription.Status.UNRESOLVED_VALUE, ProjectTypeDescription.Status.ANCHORED_REFERENCE
        )
        assertThat(descriptions.values).allSatisfy { assertThat(it.writtenAnchor).isPresent }
        val reachable = auditGraph(descriptions, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<ProjectWrittenAnchor>()).hasSize(4)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
    }

    @Test
    fun externalMetadataAndLookupsRetainOnlyDescriptors() {
        val sourceMarker = "external-metadata-retention-7c2e4b19-0d6a-4f3e-8a51-b9e3d7c2f604"
        val source = "-- $sourceMarker\nCREATE PACKAGE em AS g NUMBER; END em;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("em.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val inputColumns = mutableListOf(
            ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)),
            ProjectExternalColumn.of("INV", null, null)
        )
        val builder = ProjectExternalMetadata.builder("retention").completeSchema("HR")
            .relation(ProjectExternalRelation.of("HR", "EMPLOYEES", true, inputColumns))
        val metadata = builder.build()
        fun id(s: String) = ProjectIdentifier.fromSource(s)
        val results = mapOf(
            "metadata" to metadata,
            "found" to metadata.findColumn(id("hr"), id("employees"), id("id")),
            "absent" to metadata.findColumn(id("hr"), id("employees"), id("x")),
            "noRelation" to metadata.findRelation(id("hr"), id("jobs")),
            "unavailable" to ProjectExternalMetadata.none().findRelation(id("hr"), id("jobs"))
        )
        val reachable = auditGraph(results, listOf(snapshot, source, reader, input, builder, inputColumns), sourceMarker)
        assertThat(reachable.filterIsInstance<ProjectExternalColumn>()).hasSize(2)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectDeclarationView>()).isEmpty()
        // The source index holds no external descriptor.
        val index = (ProjectSnapshot::class.java.getDeclaredField("preparationResult").apply { isAccessible = true }
            .get(snapshot) as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val fromIndex = auditGraph(mapOf("index" to index), emptyList(), "external-metadata-unused-marker")
        assertThat(fromIndex.filterIsInstance<ProjectExternalRelation>()).isEmpty()
    }

    @Test
    fun typedCallResolutionsRetainOnlyStructuralResultsAndClassifications() {
        val sourceMarker = "typed-call-retention-6c2a9e41-3b7d-4f05-8e19-d4a0b7c3e256"
        val source = "-- $sourceMarker\nCREATE PACKAGE tc AS PROCEDURE pick(x NUMBER); PROCEDURE pick(x employees.hired%TYPE); END tc;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("tc.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val columns = mutableListOf(ProjectExternalColumn.of("HIRED", 1, ProjectExternalDatatype.of("DATE", null, null, null, null, null)))
        val builder = ProjectExternalMetadata.builder("retention").completeSchema("HR").relation(ProjectExternalRelation.of("HR", "EMPLOYEES", true, columns))
        val metadata = builder.build()
        val hr = ProjectIdentifier.fromSource("HR")
        val structural = snapshot.resolveCall(
            ProjectCallTarget.packageMember(ProjectQualifiedName.of("tc"), ProjectIdentifier.fromSource("pick")), ProjectCallKind.PROCEDURE,
            ProjectCallArguments.of(listOf(ProjectCallArgument.positional()))
        )
        val description = snapshot.describeExternalType(
            ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees", "hired"), ProjectTypeReference.Anchor.TYPE), null, metadata, hr
        )
        val described = ProjectArgumentType.described(description)
        val types = mutableListOf(described)
        val typed = snapshot.narrowCall(structural, types, metadata, hr)
        assertThat(typed.status).isEqualTo(ProjectTypedCallResolution.Status.RESOLVED)
        val reachable = auditGraph(
            mapOf("typed" to typed, "described" to described),
            listOf(snapshot, source, reader, input, builder, columns, metadata, description, types), sourceMarker
        )
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectExternalMetadata>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectExternalTypeDescription>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectTypeDescription>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectCallResolution>()).containsExactly(structural)
    }

    @Test
    fun externalTypeDescriptionsRetainOnlyDescriptionsAndDescriptors() {
        val sourceMarker = "external-type-retention-1e7c4a93-5b20-4f8d-9c61-3a0e8d2b7f45"
        val source = "-- $sourceMarker\nCREATE PACKAGE et AS TYPE rows_t IS TABLE OF employees%ROWTYPE; g NUMBER; " +
            "CURSOR c RETURN rows_t; TYPE c_rows IS TABLE OF c%ROWTYPE; END et;\n/\nCREATE PACKAGE eq AS x et.g%TYPE; END eq;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("et.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val columns = mutableListOf(ProjectExternalColumn.of("ID", 1, ProjectExternalDatatype.of("NUMBER", null, null, null, 8, 0)))
        val builder = ProjectExternalMetadata.builder("retention").completeSchema("HR")
            .relation(ProjectExternalRelation.of("HR", "EMPLOYEES", true, columns))
            .relation(ProjectExternalRelation.of("HR", "ET", true, columns))
        val metadata = builder.build()
        val hr = ProjectIdentifier.fromSource("HR")
        fun element(name: String) = (snapshot.findDeclarations(ProjectQualifiedName.of("et", name)).declarations.single() as ProjectShapedTypeView).elementType.get()
        val x = snapshot.findDeclarations(ProjectQualifiedName.of("eq", "x")).declarations.single() as ProjectPackageValueView
        val results = mapOf(
            "resolved" to snapshot.describeExternalType(element("rows_t"), ProjectQualifiedName.of("et"), metadata, hr),
            "captured" to snapshot.describeExternalType(element("c_rows"), ProjectQualifiedName.of("et"), metadata, hr),
            "conflict" to snapshot.describeExternalType(x.typeRef, ProjectQualifiedName.of("eq"), metadata, hr),
            "written" to snapshot.describeExternalType(ProjectWrittenAnchor.of(ProjectQualifiedName.of("employees"), ProjectTypeReference.Anchor.ROWTYPE), null, metadata, hr),
            "unavailable" to snapshot.describeExternalType(element("rows_t"), ProjectQualifiedName.of("et"), ProjectExternalMetadata.none(), hr)
        )
        assertThat(results.mapValues { it.value.status.name }).containsEntry("resolved", "RESOLVED").containsEntry("captured", "NOT_EXTERNAL")
            .containsEntry("conflict", "CONFLICT").containsEntry("written", "RESOLVED").containsEntry("unavailable", "UNAVAILABLE")
        val reachable = auditGraph(results, listOf(snapshot, source, reader, input, builder, columns, metadata), sourceMarker)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectExternalMetadata>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<ReferenceHolder>()).isEmpty()
        assertThat(reachable.filterIsInstance<AnchorTrace>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectExternalRelation>()).isNotEmpty()
    }

    @Test
    fun memberNameCoverageRetainsOnlyNamesAndRanges() {
        val sourceMarker = "member-name-retention-5a8c2e71-9d34-4b6f-a0e2-7c1b3d9f4e58"
        val source = "-- $sourceMarker\nCREATE PACKAGE mn AS CURSOR c RETURN t; e EXCEPTION; g NUMBER b; " +
            "TYPE rows_t IS TABLE OF c%ROWTYPE; \$IF \$\$x \$THEN \$END END mn;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("mn.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val index = (ProjectSnapshot::class.java.getDeclaredField("preparationResult").apply { isAccessible = true }
            .get(snapshot) as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val spec = index.declarations.filterIsInstance<com.felipebz.zpa.project.PackageDeclaration>().single()
        val unit = index.occurrencesFor(spec.fileId).single { it.declaration === spec }
        val evidence = com.felipebz.zpa.project.PackageMemberNames.at(
            index, unit, com.felipebz.zpa.project.OracleIdentifier.fromSource("c"), null
        )
        val reachable = auditGraph(mapOf("coverage" to spec.memberNames!!, "evidence" to evidence), listOf(snapshot, source, reader, input), sourceMarker)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageMemberName>()).hasSize(1)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectDeclaration>()).isEmpty()
    }

    @Test
    fun valueResolutionsRetainOnlyDurableFacts() {
        val sourceMarker = "value-resolution-retention-6d1f3a92-7e48-4b0c-a5d9-1c8e2b40f7a3"
        val source = "-- $sourceMarker\nCREATE PACKAGE vr AS g NUMBER; d NUMBER; d DATE; PROCEDURE only_proc; END vr;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("vr.sql", reader)
        val failedReader = ProjectSourceReader { throw IllegalStateException("failure: $sourceMarker") }
        val failedInput = ProjectSourceInput.ofReader("bad.sql", failedReader)
        val snapshot = ProjectSnapshots.prepare(listOf(input, failedInput), false)
        val complete = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input, failedReader, failedInput)
        val owner = ProjectQualifiedName.of("vr")
        fun r(s: ProjectSnapshot, n: String) = s.resolvePackageValue(ProjectQualifiedName.of(n), owner)
        val results = mapOf(
            "resolved" to r(complete, "g"), "ambiguous" to r(complete, "d"), "notFound" to r(complete, "only_proc"),
            "incomplete" to r(snapshot, "g")
        )
        assertThat(results.values.map { it.status }).containsExactly(
            ProjectValueResolution.Status.RESOLVED, ProjectValueResolution.Status.AMBIGUOUS,
            ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT, ProjectValueResolution.Status.INCOMPLETE_INDEX
        )
        val reachable = auditGraph(results, forbidden + complete, sourceMarker)
        assertThat(reachable.filterIsInstance<PackageValueDeclaration>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
    }

    @Test
    fun typeDescriptionsRetainOnlyDurableFacts() {
        val sourceMarker = "description-retention-2b8d6e41-93c7-4a50-8e1b-5c4f0a7d9e26"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE dv AS SUBTYPE m IS NUMBER; SUBTYPE a IS m; SUBTYPE t IS a; SUBTYPE x IS ext.t; " +
            "SUBTYPE c1 IS c2; SUBTYPE c2 IS c1; TYPE r IS RECORD (n t); END dv;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("dv.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)
        val owner = ProjectQualifiedName.of("dv")
        fun describe(name: String) = snapshot.describeType(ProjectQualifiedName.of(name), owner)

        val descriptions = mapOf(
            "builtIn" to describe("NUMBER"), "chain" to describe("t"), "declared" to describe("r"),
            "unresolved" to describe("x"), "cycle" to describe("c1")
        )
        assertThat(descriptions.values.map { it.status }).containsExactly(
            ProjectTypeDescription.Status.BUILT_IN, ProjectTypeDescription.Status.BUILT_IN, ProjectTypeDescription.Status.DECLARED_TYPE,
            ProjectTypeDescription.Status.UNRESOLVED_NAME, ProjectTypeDescription.Status.CYCLE
        )
        val reachable = auditGraph(descriptions, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<PackageSubtypeDeclaration>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(descriptions.getValue("chain").subtypes).hasSize(3)
        assertThatThrownBy { (descriptions.getValue("chain").subtypes as MutableList<ProjectSubtypeView>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)

        // The terminal reference of a description reaches no internal fact or TypeRef.
        val referenceOnly = auditGraph(mapOf<String, Any?>("terminal" to descriptions.getValue("chain").terminalReference), forbidden, sourceMarker)
        assertThat(referenceOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(referenceOnly.filterIsInstance<TypeRef>()).isEmpty()
    }

    @Test
    fun packageValueViewsRetainOnlyDurableFacts() {
        val sourceMarker = "value-view-retention-9c3e1b52-4d6a-4f0e-8a77-2b5d7e1c9f03"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE vv AS g e.c%TYPE; k CONSTANT NUMBER := 1; END vv;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("vv.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)

        val variable = snapshot.findDeclarations(ProjectQualifiedName.of("vv", "g")).declarations.single()
            as ProjectPackageValueView
        val constant = snapshot.findDeclarations(ProjectQualifiedName.of("vv", "k")).declarations.single()
            as ProjectPackageValueView
        val roots = mapOf<String, Any?>(
            "variable" to variable, "constant" to constant, "owner" to variable.owner,
            "localName" to variable.localName, "typeRef" to variable.typeRef, "range" to variable.sourceRange
        )
        val reachable = auditGraph(roots, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<PackageValueDeclaration>()).hasSize(2)
        assertThat(reachable.filterIsInstance<TypeRef>()).isNotEmpty()
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()

        val idOnly = auditGraph(mapOf("id" to variable.id), forbidden, sourceMarker)
        assertThat(idOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        val publicValues = auditGraph(
            mapOf("owner" to variable.owner, "name" to variable.localName, "ref" to variable.typeRef), forbidden, sourceMarker
        )
        assertThat(publicValues.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(publicValues.filterIsInstance<TypeRef>()).isEmpty()
    }

    @Test
    fun callableViewsParametersAndReferencesRetainOnlyDurableFacts() {
        val sourceMarker = "callable-retention-6a8e2f10-77c1-4d0b-b0a4-1f3c9e5d2b77"
        val source = "-- $sourceMarker\n" +
            "CREATE PACKAGE cv AS PROCEDURE pr(a IN NUMBER, b OUT NOCOPY e.c%TYPE DEFAULT NULL); " +
            "FUNCTION fn(x REF o) RETURN NUMBER DETERMINISTIC; END cv;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("cv.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }.get(snapshot)
        val index = (resultField as com.felipebz.zpa.project.ProjectIndexPreparationResult).index
        val forbidden = listOf(snapshot, resultField, index, source, reader, input)

        val proc = snapshot.findDeclarations(ProjectQualifiedName.of("cv", "pr")).declarations.single()
            as ProjectPackageSubprogramView
        val fn = snapshot.findDeclarations(ProjectQualifiedName.of("cv", "fn")).declarations.single()
            as ProjectFunctionView
        val roots = mapOf<String, Any?>(
            "proc" to proc, "fn" to fn, "procParams" to proc.parameters, "fnParams" to fn.parameters,
            "paramRefs" to proc.parameters.map { it.typeRef }, "return" to fn.returnType,
            "owner" to proc.packageOwner, "modes" to proc.parameters.map { it.mode }
        )
        val reachable = auditGraph(roots, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageProcedureDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<PackageFunctionDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectParameter>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectTypeReference>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()

        val standalone = auditGraph(
            mapOf("refs" to proc.parameters.map { it.typeRef } + fn.returnType), forbidden, sourceMarker
        )
        assertThat(standalone.filterIsInstance<TypeRef>()).isEmpty()
        assertThat(standalone.filterIsInstance<ProjectDeclaration>()).isEmpty()
    }

    @Test
    fun namedTypeResolutionsRetainOnlyDurableFacts() {
        val sourceMarker = "resolution-retention-2d7b9c41-5e0f-4a9e-8c33-9f1e6b0a4d52"
        val source = "-- $sourceMarker\nCREATE PACKAGE rr AS TYPE t IS RECORD (a NUMBER); END rr;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("a.sql", reader)
        val dup = ProjectSourceInput.ofReader("b.sql", reader)
        val failed = ProjectSourceInput.ofReader("f.sql", ProjectSourceReader { throw IllegalStateException(sourceMarker) })
        val snapshot = ProjectSnapshots.prepare(listOf(input, dup, failed), false)
        val single = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult")
            .apply { isAccessible = true }
        val forbidden = listOf(snapshot, single, resultField.get(snapshot), resultField.get(single),
            (resultField.get(snapshot) as com.felipebz.zpa.project.ProjectIndexPreparationResult).index,
            source, reader, input, dup, failed)

        val owner = ProjectQualifiedName.of("rr")
        val resolved = single.resolveNamedType(ProjectQualifiedName.of("t"), owner)
        val ambiguous = ProjectSnapshots.prepare(listOf(input, dup), false)
            .resolveNamedType(ProjectQualifiedName.of("rr", "t"))
        val incomplete = snapshot.resolveNamedType(ProjectQualifiedName.of("rr", "t"))
        assertThat(resolved.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(ambiguous.status).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS)
        assertThat(incomplete.status).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX)

        val reachable = auditGraph(
            mapOf("resolved" to resolved, "ambiguous" to ambiguous, "incomplete" to incomplete),
            forbidden, sourceMarker
        )
        assertThat(reachable.filterIsInstance<PackageTypeDeclaration>()).hasSize(5)
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()

        val idOnly = auditGraph(mapOf("id" to resolved.declaration.get().id), forbidden, sourceMarker)
        assertThat(idOnly.filterIsInstance<ProjectDeclaration>()).isEmpty()
        assertThat(idOnly.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun packageSubprogramInventoriesRetainOnlyDurableFacts() {
        val sourceMarker = "inventory-callable-retention-9b3e1d57-0c2a-4f88-a1b6-7d4e2c9f0a13"
        val source = "-- $sourceMarker\nCREATE PACKAGE ci AS PROCEDURE go(a IN NUMBER); FUNCTION go RETURN NUMBER; END ci;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("c.sql", reader)
        val failed = ProjectSourceInput.ofReader("f.sql", ProjectSourceReader { throw IllegalStateException(sourceMarker) })
        val complete = ProjectSnapshots.prepare(listOf(input), false)
        val incompleteSnapshot = ProjectSnapshots.prepare(listOf(input, failed), false)
        val field = ProjectSnapshot::class.java.getDeclaredField("preparationResult").apply { isAccessible = true }
        val forbidden = listOf(
            complete, incompleteSnapshot, field.get(complete), field.get(incompleteSnapshot),
            (field.get(complete) as com.felipebz.zpa.project.ProjectIndexPreparationResult).index,
            (field.get(incompleteSnapshot) as com.felipebz.zpa.project.ProjectIndexPreparationResult).index,
            source, reader, input, failed
        )
        val owner = ProjectQualifiedName.of("ci")
        val name = ProjectIdentifier.fromSource("go")
        val full = complete.findPackageSubprograms(owner, name)
        val partial = incompleteSnapshot.findPackageSubprograms(owner, name)
        assertThat(full.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        assertThat(partial.status).isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX)

        val reachable = auditGraph(mapOf("full" to full, "partial" to partial), forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageProcedureDeclaration>()).hasSize(2)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageFunctionDeclaration>()).hasSize(2)
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()

        val idOnly = auditGraph(mapOf("id" to full.declarations.first().id), forbidden, sourceMarker)
        assertThat(idOnly.filterIsInstance<com.felipebz.zpa.project.ProjectDeclaration>()).isEmpty()
        assertThat(idOnly.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun specificationCorrelationsRetainOnlyDurableFacts() {
        val sourceMarker = "correlation-retention-4c1a7e92-6b03-4d15-9e7a-2f8d0b5c3a61"
        val spec = "-- $sourceMarker\nCREATE PACKAGE cr AS PROCEDURE go(a NUMBER); END cr;"
        val body = "CREATE PACKAGE BODY cr AS PROCEDURE go(a NUMBER) IS BEGIN NULL; END; END cr;"
        val readers = listOf(ProjectSourceReader { spec }, ProjectSourceReader { spec }, ProjectSourceReader { body })
        val inputs = listOf(
            ProjectSourceInput.ofReader("s1.sql", readers[0]), ProjectSourceInput.ofReader("s2.sql", readers[1]),
            ProjectSourceInput.ofReader("b.sql", readers[2])
        )
        val failed = ProjectSourceInput.ofReader("f.sql", ProjectSourceReader { throw IllegalStateException(sourceMarker) })
        val resolvedSnap = ProjectSnapshots.prepare(listOf(inputs[0], inputs[2]), false)
        val ambiguousSnap = ProjectSnapshots.prepare(inputs, false)
        val incompleteSnap = ProjectSnapshots.prepare(inputs + failed, false)
        val field = ProjectSnapshot::class.java.getDeclaredField("preparationResult").apply { isAccessible = true }
        val snaps = listOf(resolvedSnap, ambiguousSnap, incompleteSnap)
        val forbidden = snaps + snaps.flatMap {
            val r = field.get(it) as com.felipebz.zpa.project.ProjectIndexPreparationResult
            listOf(r, r.index)
        } + readers + inputs + failed + spec + body

        fun corr(s: ProjectSnapshot) = s.correlateSpecification(
            s.declarations.filterIsInstance<ProjectPackageSubprogramView>().single { it.role == ProjectDeclarationRole.BODY }.id
        )
        val resolved = corr(resolvedSnap)
        val ambiguous = corr(ambiguousSnap)
        val incomplete = corr(incompleteSnap)
        assertThat(resolved.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(ambiguous.status).isEqualTo(ProjectSpecificationCorrelation.Status.AMBIGUOUS)
        assertThat(incomplete.status).isEqualTo(ProjectSpecificationCorrelation.Status.INCOMPLETE)

        val reachable = auditGraph(
            mapOf("resolved" to resolved, "ambiguous" to ambiguous, "incomplete" to incomplete), forbidden, sourceMarker
        )
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageProcedureDeclaration>()).hasSize(8)
        assertThat(reachable.filterIsInstance<ProjectPreparationFailure>()).hasSize(1)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSubprogramSpecificationResolver>()).isEmpty()

        val idOnly = auditGraph(mapOf("id" to resolved.candidates.single().id), forbidden, sourceMarker)
        assertThat(idOnly.filterIsInstance<com.felipebz.zpa.project.ProjectDeclaration>()).isEmpty()
        assertThat(idOnly.filterIsInstance<ProjectSnapshot>()).isEmpty()
    }

    @Test
    fun scanSnapshotDoesNotRetainTheScanAdapterChecksOrAst() {
        val sourceMarker = "scan-snapshot-retention-8e5d2b60-1a47-4c39-b0f2-3d9a6e1c7f84"
        val body = "-- $sourceMarker\nCREATE PACKAGE BODY sr AS PROCEDURE go IS BEGIN NULL; END go; END sr;"
        val spec = "CREATE PACKAGE sr AS PROCEDURE go; END sr;"
        val reader = ProjectSourceReader { spec }
        val result = com.felipebz.zpa.project.ProjectIndexPreparation().prepare(
            listOf(
                com.felipebz.zpa.project.ProjectSource(com.felipebz.zpa.project.FileId("s.sql")) { spec },
                com.felipebz.zpa.project.ProjectSource(com.felipebz.zpa.project.FileId("b.sql")) { body }
            ), concurrent = false
        )
        val context = com.felipebz.zpa.project.ProjectAnalysisContext.prepared(result)
        val check = CapturingCheck()
        val file = object : PlSqlFile {
            override fun contents() = body
            override fun fileName() = "b.sql"
            override fun path() = java.nio.file.Path.of("b.sql")
            override fun type() = PlSqlFile.Type.MAIN
        }
        com.felipebz.zpa.squid.AstScanner(emptyList(), null, true, java.nio.charset.StandardCharsets.UTF_8, context)
            .scanFile(file, listOf(check), com.felipebz.zpa.project.FileId("b.sql"))
        val snapshot = check.snapshot!!
        val analysis = check.analysis!!
        val bodyId = snapshot.declarations.first { it.role == ProjectDeclarationRole.BODY }.id
        val correlation = snapshot.correlateSpecification(bodyId)

        val forbidden = listOf(analysis, check, body, spec, reader, context, check.node!!, check.node!!.token)
        val reachable = auditGraph(
            mapOf("snapshot" to snapshot, "correlation" to correlation, "id" to bodyId),
            forbidden, sourceMarker
        )
        assertThat(reachable.filterIsInstance<ProjectAnalysis>()).isEmpty()
        assertThat(reachable.filterIsInstance<PlSqlCheck>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.flr.api.AstNode>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).hasSize(1)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.PackageProcedureDeclaration>()).hasSize(2)
    }

    @Test
    fun unpreparedScanSnapshotAndItsResultsRetainNothingTransient() {
        val sourceMarker = "unprepared-retention-0f6b3a28-d1c5-47e9-8a14-5b7e9c2d4f60"
        val body = "-- $sourceMarker\nCREATE PACKAGE BODY ur AS PROCEDURE go IS BEGIN NULL; END go; END ur;"
        val check = CapturingCheck()
        val file = object : PlSqlFile {
            override fun contents() = body
            override fun fileName() = "b.sql"
            override fun path() = java.nio.file.Path.of("b.sql")
            override fun type() = PlSqlFile.Type.MAIN
        }
        com.felipebz.zpa.squid.AstScanner(emptyList(), null, true, java.nio.charset.StandardCharsets.UTF_8)
            .scanFile(file, listOf(check), com.felipebz.zpa.project.FileId("b.sql"))
        val snapshot = check.snapshot!!
        val roots = mapOf<String, Any?>(
            "snapshot" to snapshot,
            "declarations" to snapshot.declarations,
            "perFile" to snapshot.declarationsFor("b.sql"),
            "byName" to snapshot.findDeclarations(ProjectQualifiedName.of("ur", "go")),
            "type" to snapshot.resolveNamedType(ProjectQualifiedName.of("t"), ProjectQualifiedName.of("ur")),
            "subprograms" to snapshot.findPackageSubprograms(ProjectQualifiedName.of("ur"), ProjectIdentifier.fromSource("go"))
        )
        val reachable = auditGraph(roots, listOf(check.analysis!!, check, body, check.node!!), sourceMarker)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).hasSize(1)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectDeclaration>()).isEmpty()
        assertThat(reachable.filterIsInstance<ProjectAnalysis>()).isEmpty()
    }

    private class CapturingCheck : PlSqlCheck() {
        var snapshot: ProjectSnapshot? = null
        var analysis: ProjectAnalysis? = null
        var node: com.felipebz.flr.api.AstNode? = null

        init {
            subscribeTo(com.felipebz.zpa.api.PlSqlGrammar.PROCEDURE_DECLARATION)
        }

        override fun visitNode(node: com.felipebz.flr.api.AstNode) {
            this.node = node
            analysis = projectAnalysis()
            snapshot = projectAnalysis().snapshot
        }
    }

    @Test
    fun standaloneViewsParametersAndReferencesRetainOnlyDurableFacts() {
        val sourceMarker = "standalone-retention-3b9d6a15-82f4-4c7e-a0d1-6e5f2b8c1d40"
        val source = "-- $sourceMarker\nCREATE PROCEDURE sp(a IN NUMBER, b OUT NOCOPY t.c%TYPE) IS BEGIN NULL; END;\n/\n" +
            "CREATE FUNCTION sf(x NUMBER DEFAULT 1) RETURN REF o DETERMINISTIC IS BEGIN RETURN NULL; END;"
        val reader = ProjectSourceReader { source }
        val input = ProjectSourceInput.ofReader("st.sql", reader)
        val snapshot = ProjectSnapshots.prepare(listOf(input), false)
        val resultField = ProjectSnapshot::class.java.getDeclaredField("preparationResult").apply { isAccessible = true }
        val result = resultField.get(snapshot) as com.felipebz.zpa.project.ProjectIndexPreparationResult
        val forbidden = listOf(snapshot, resultField.get(snapshot), result.index, source, reader, input)

        val procedure = snapshot.findDeclarations(ProjectQualifiedName.of("sp")).declarations.single() as ProjectSubprogramView
        val function = snapshot.findDeclarations(ProjectQualifiedName.of("sf")).declarations.single() as ProjectFunctionView
        val roots = mapOf<String, Any?>(
            "procedure" to procedure, "function" to function, "procedureParameters" to procedure.parameters,
            "functionParameters" to function.parameters, "parameterRefs" to procedure.parameters.map { it.typeRef },
            "return" to function.returnType, "localName" to procedure.localName, "qualifiedName" to function.qualifiedName
        )
        val reachable = auditGraph(roots, forbidden, sourceMarker)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.StandaloneProcedureDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.StandaloneFunctionDeclaration>()).hasSize(1)
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectParameter>()).hasSize(3)
        assertThat(reachable.filterIsInstance<ProjectSnapshot>()).isEmpty()
        assertThat(reachable.filterIsInstance<com.felipebz.zpa.project.ProjectSymbolIndex>()).isEmpty()

        // Projected references and IDs are independent of the internal TypeRef and of the facts.
        val standalone = auditGraph(
            mapOf("refs" to procedure.parameters.map { it.typeRef } + function.returnType, "id" to procedure.id), forbidden, sourceMarker
        )
        assertThat(standalone.filterIsInstance<com.felipebz.zpa.project.TypeRef>()).isEmpty()
        assertThat(standalone.filterIsInstance<com.felipebz.zpa.project.ProjectDeclaration>()).isEmpty()
    }

    @Test
    fun auditFollowsContainersPrivateAnyFieldsAndCapturedLambdaFieldsByIdentity() {
        val sentinel = Any()
        val capture = { sentinel }
        val holder = OpaqueFieldHolder(Optional.of(mapOf("capture" to arrayOf(listOf(capture)))))

        assertThatThrownBy {
            auditGraph(mapOf("holder" to holder), listOf(sentinel), "unused-source-marker")
        }.isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Forbidden retained identity")
            .hasMessageContaining("holder")
            .hasMessageContaining(".payload")
            .hasMessageContaining(sentinel.javaClass.name)
    }

    @Test
    fun auditRejectsTheUnitDispatcherReachableFromAGraph() {
        val dispatcher = com.felipebz.zpa.project.extraction.UnitDispatcher(com.felipebz.zpa.project.FileId("f.sql"), emptyList(), com.felipebz.zpa.project.extraction.DiscardedSourceText(""))
        assertThatThrownBy {
            auditGraph(mapOf("holder" to OpaqueFieldHolder(dispatcher)), emptyList(), "unused-source-marker")
        }.isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Forbidden transient object")
            .hasMessageContaining("com.felipebz.zpa.project.extraction.UnitDispatcher")
    }

    @Test
    fun auditRejectsExtractionComponentsReachableFromAGraph() {
        val tokens = com.felipebz.zpa.project.extraction.TokenStream(com.felipebz.zpa.project.FileId("f.sql"), emptyList())
        assertThatThrownBy {
            auditGraph(mapOf("holder" to OpaqueFieldHolder(tokens)), emptyList(), "unused-source-marker")
        }.isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Forbidden transient object")
            .hasMessageContaining("com.felipebz.zpa.project.extraction.TokenStream")
    }

    private class OpaqueFieldHolder(private val payload: Any)

    /**
     * JDK containers are followed through their contents, never their inaccessible fields.
     * Only enumerated scalar values terminate traversal; application fields are inspected
     * regardless of their declared type, including synthetic lambda captures and enums.
     */
    private fun auditGraph(
        roots: Map<String, Any?>,
        forbiddenIdentities: List<Any>,
        sourceMarker: String
    ): List<Any> {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val reachable = mutableListOf<Any>()

        fun visit(value: Any?, edge: String) {
            if (value == null || !visited.add(value)) return
            val type = value.javaClass
            fun reject(reason: String): Nothing = throw AssertionError("$reason: ${type.name} at $edge")
            if (forbiddenIdentities.any { it === value }) reject("Forbidden retained identity")
            if (value is String && value.contains(sourceMarker)) reject("Retained source/comment contents")
            if (isTransient(value)) reject("Forbidden transient object")
            reachable += value

            if (type == String::class.java || type in scalarTypes) return
            if (type.isArray) {
                for (index in 0 until ReflectArray.getLength(value)) {
                    visit(ReflectArray.get(value, index), "$edge[$index]")
                }
                return
            }
            when (value) {
                is Map<*, *> -> value.entries.forEachIndexed { index, entry ->
                    visit(entry.key, "$edge.keys[$index]")
                    visit(entry.value, "$edge.values[$index]")
                }
                is Collection<*> -> value.forEachIndexed { index, element -> visit(element, "$edge[$index]") }
                is Optional<*> -> {
                    if (value.isPresent) visit(value.get(), "$edge.value")
                    return
                }
            }
            if (isSafeJdkContainer(value)) return

            var owner: Class<*>? = type
            while (owner != null && owner != Any::class.java && owner != Enum::class.java) {
                val declaringType = owner
                if (!isInspectableApplicationType(declaringType)) reject("Unexpected opaque non-value object")
                for (field in declaringType.declaredFields.sortedBy { it.name }) {
                    if (Modifier.isStatic(field.modifiers)) continue
                    val fieldEdge = "$edge.${declaringType.simpleName}.${field.name}"
                    if (!field.trySetAccessible()) {
                        throw AssertionError("Inaccessible application field: ${declaringType.name} at $fieldEdge")
                    }
                    visit(field.get(value), fieldEdge)
                }
                owner = declaringType.superclass
            }
            if (type == Any::class.java) reject("Unexpected opaque non-value object")
        }

        roots.forEach { (name, root) -> visit(root, name) }
        return reachable
    }

    private fun isTransient(value: Any): Boolean {
        val name = value.javaClass.name
        return value is ProjectSourceInput || value is ProjectSourceReader ||
            value is ProjectSource || value is com.felipebz.zpa.project.ProjectSourceReader ||
            value is ProjectIndexPreparation || value is ProjectSymbolIndexBuilder ||
            value is ProjectDeclarationSourceExtractor || value is Symbol || value is Scope ||
            value is SymbolTable || value is PlSqlDatatype || value is Throwable ||
            value is PlSqlVisitor || value is PlSqlFile ||
            value is Buffer || value is CharArray || value is ByteArray ||
            value is CharSequence && value !is String ||
            value is java.io.Reader || value is java.io.InputStream ||
            value is Queue<*> || value is BaseStream<*, *> ||
            value is Iterator<*> || value is java.util.Spliterator<*> ||
            name.startsWith("com.felipebz.flr.") ||
            name.startsWith("com.felipebz.zpa.lexer.") ||
            name.startsWith("com.felipebz.zpa.parser.") ||
            name.startsWith("com.felipebz.zpa.symbols.") ||
            name.startsWith("com.felipebz.zpa.squid.") ||
            name.startsWith("com.felipebz.zpa.checks.") ||
            name.startsWith("com.felipebz.zpa.api.checks.") ||
            name.startsWith("com.felipebz.zpa.api.syntax.") ||
            name.startsWith("com.felipebz.zpa.project.extraction.")
    }

    private fun isInspectableApplicationType(type: Class<*>): Boolean =
        type.name.startsWith("com.felipebz.") || type == Pair::class.java ||
            type == Triple::class.java || type.name == "kotlin.jvm.internal.Lambda"

    private fun isSafeJdkContainer(value: Any): Boolean {
        if (value !is Collection<*> && value !is Map<*, *>) return false
        val name = value.javaClass.name
        return name in safeContainerTypes
    }

    private val safeContainerTypes = setOf(
        "java.util.ArrayList", "java.util.LinkedHashMap", "java.util.HashMap",
        "java.util.HashSet", "java.util.LinkedHashSet", "java.util.Arrays\$ArrayList",
        "java.util.Collections\$EmptyList", "java.util.Collections\$EmptySet",
        "java.util.Collections\$EmptyMap", "java.util.Collections\$SingletonList",
        "java.util.Collections\$SingletonSet", "java.util.Collections\$SingletonMap",
        "java.util.Collections\$UnmodifiableCollection", "java.util.Collections\$UnmodifiableList",
        "java.util.Collections\$UnmodifiableRandomAccessList", "java.util.Collections\$UnmodifiableSet",
        "java.util.Collections\$UnmodifiableMap",
        "java.util.ImmutableCollections\$List12", "java.util.ImmutableCollections\$ListN",
        "java.util.ImmutableCollections\$Set12", "java.util.ImmutableCollections\$SetN",
        "java.util.ImmutableCollections\$Map1", "java.util.ImmutableCollections\$MapN",
        "kotlin.collections.EmptyList", "kotlin.collections.EmptySet", "kotlin.collections.EmptyMap"
    )

    private val scalarTypes = setOf(
        Boolean::class.javaObjectType, Byte::class.javaObjectType, Short::class.javaObjectType,
        Int::class.javaObjectType, Long::class.javaObjectType, Float::class.javaObjectType,
        Double::class.javaObjectType, Char::class.javaObjectType, OptionalInt::class.java
    )
}
