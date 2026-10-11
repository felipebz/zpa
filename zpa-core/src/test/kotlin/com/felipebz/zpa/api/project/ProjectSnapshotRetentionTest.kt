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
import com.felipebz.zpa.api.checks.PlSqlVisitor
import com.felipebz.zpa.api.symbols.Scope
import com.felipebz.zpa.api.symbols.Symbol
import com.felipebz.zpa.api.symbols.SymbolTable
import com.felipebz.zpa.api.symbols.datatype.PlSqlDatatype
import com.felipebz.zpa.project.PackageFunctionDeclaration
import com.felipebz.zpa.project.PackageTypeDeclaration
import com.felipebz.zpa.project.ProjectDeclaration
import com.felipebz.zpa.project.ProjectDeclarationSourceExtractor
import com.felipebz.zpa.project.ProjectIndexPreparation
import com.felipebz.zpa.project.ProjectParameter
import com.felipebz.zpa.project.ProjectRecordField
import com.felipebz.zpa.project.ProjectSource
import com.felipebz.zpa.project.ProjectSymbolIndexBuilder
import com.felipebz.zpa.project.QualifiedName
import com.felipebz.zpa.project.SourceRange
import com.felipebz.zpa.project.TypeRef
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Modifier
import java.nio.Buffer
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Optional
import java.util.Queue
import java.util.stream.BaseStream

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
            name.startsWith("com.felipebz.zpa.project.ProjectDeclarationExtractor$")
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
        Double::class.javaObjectType, Char::class.javaObjectType
    )
}
