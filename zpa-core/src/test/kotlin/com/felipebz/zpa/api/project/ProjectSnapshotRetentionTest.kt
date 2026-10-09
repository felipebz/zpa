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
