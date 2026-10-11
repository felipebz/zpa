package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.ProjectDeclarationId
import com.felipebz.zpa.api.project.ProjectDeclarationKind
import com.felipebz.zpa.api.project.ProjectDeclarationRole
import com.felipebz.zpa.api.project.ProjectFunctionView
import com.felipebz.zpa.api.project.ProjectIdentifier
import com.felipebz.zpa.api.project.ProjectInventory
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution
import com.felipebz.zpa.api.project.ProjectPackageSubprogramView
import com.felipebz.zpa.api.project.ProjectPackageTypeView
import com.felipebz.zpa.api.project.ProjectParameterMode
import com.felipebz.zpa.api.project.ProjectPreparationState
import com.felipebz.zpa.api.project.ProjectQualifiedName
import com.felipebz.zpa.api.project.ProjectSnapshot
import com.felipebz.zpa.api.project.ProjectSnapshots
import com.felipebz.zpa.api.project.ProjectSourceInput
import com.felipebz.zpa.api.project.ProjectSourceReader
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation
import com.felipebz.zpa.api.project.ProjectSubtypeView
import com.felipebz.zpa.api.project.ProjectTypeReference
import com.felipebz.zpa.api.project.ProjectTypeShape
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** Uses only the supported com.felipebz.zpa.api.project contract, compiled against the zpa-core jar. */
@OptIn(ZpaExperimentalApi::class)
class ProjectApiKotlinConsumerTest {
    private val reads = AtomicInteger()

    private fun sql(name: String) = checkNotNull(javaClass.getResourceAsStream("/sql/$name")).use { String(it.readAllBytes()) }

    private fun counted(id: String, resource: String) = ProjectSourceInput.ofReader(id, ProjectSourceReader {
        reads.incrementAndGet()
        sql(resource)
    })

    private fun failing(id: String) = ProjectSourceInput.ofReader(id, ProjectSourceReader { throw IOException("cannot read $id") })

    private fun project(vararg extra: ProjectSourceInput): ProjectSnapshot = ProjectSnapshots.prepare(
        listOf(counted("db/shop_spec.sql", "shop_spec.sql"), counted("db/shop_body.sql", "shop_body.sql")) + extra, false
    )

    private fun n(vararg s: String) = ProjectQualifiedName.of(*s)
    private fun id(s: String) = ProjectIdentifier.fromSource(s)

    private fun bodyId(snapshot: ProjectSnapshot, name: String): ProjectDeclarationId =
        snapshot.findPackageSubprograms(n("shop"), id(name)).declarations
            .first { it.role == ProjectDeclarationRole.BODY }.id

    @Test
    fun preparesAndInspectsDeclarations() {
        val snapshot = project()
        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        assertThat(snapshot.attemptedFileCount).isEqualTo(2)
        assertThat(snapshot.fileIds).containsExactly("db/shop_body.sql", "db/shop_spec.sql")
        val kinds = snapshot.declarations.groupingBy { it.kind }.eachCount()
        assertThat(kinds[ProjectDeclarationKind.PACKAGE]).isEqualTo(1)
        assertThat(kinds[ProjectDeclarationKind.PACKAGE_TYPE]).isEqualTo(1)
        assertThat(kinds[ProjectDeclarationKind.PACKAGE_SUBTYPE]).isEqualTo(1)
        assertThat(kinds[ProjectDeclarationKind.PACKAGE_PROCEDURE]).isEqualTo(5)
        assertThat(kinds[ProjectDeclarationKind.PACKAGE_FUNCTION]).isEqualTo(2)
        assertThat(snapshot.declarationsFor("db/shop_spec.sql").declarations).hasSize(7)
        assertThat(snapshot.declarationsFor("unknown.sql").declarations).isEmpty()
    }

    @Test
    fun resolvesRecordTypeAndInspectsOrderedFieldsAndSubtype() {
        val snapshot = project()
        val resolution = snapshot.resolveNamedType(n("order_t"), n("shop"))
        assertThat(resolution.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        val type = resolution.declaration.orElseThrow() as ProjectPackageTypeView
        assertThat(type.shape.orElseThrow()).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(type.recordFields.map { it.name.originalSpelling }).containsExactly("id", "customer", "total", "note")
        assertThat(type.recordFields.map { it.ordinal }).containsExactly(0, 1, 2, 3)
        val note = type.recordFields[3].typeRef
        assertThat(note.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(note.anchor.orElseThrow()).isEqualTo(ProjectTypeReference.Anchor.TYPE)
        assertThat(type.recordFields[0].typeRef.anchor).isEmpty

        assertThat(snapshot.resolveNamedType(n("shop", "order_t")).declaration.orElseThrow().id).isEqualTo(type.id)
        assertThat(snapshot.findDeclarations(n("shop", "order_t")).declarations.single().id).isEqualTo(type.id)
        assertThat(snapshot.resolveNamedType(n("order_t")).status)
            .isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)

        val money = snapshot.resolveNamedType(n("money_t"), n("shop")).declaration.orElseThrow() as ProjectSubtypeView
        assertThat(money.baseType.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(money.baseType.name.last.originalSpelling).isEqualTo("NUMBER")
    }

    @Test
    fun inspectsOverloadCandidatesAndSignaturesWithoutRanking() {
        val snapshot = project()
        val place = snapshot.findPackageSubprograms(n("shop"), id("place"))
        assertThat(place.status).isEqualTo(ProjectInventory.Status.COMPLETE)
        assertThat(place.declarations.map { it.role }).containsExactly(
            ProjectDeclarationRole.BODY, ProjectDeclarationRole.SPECIFICATION, ProjectDeclarationRole.SPECIFICATION
        )
        val twoArgs = place.declarations[1]
        assertThat(twoArgs.packageOwner).isEqualTo(n("shop"))
        assertThat(twoArgs.parameters.map { it.name.originalSpelling }).containsExactly("o", "notify")
        assertThat(twoArgs.parameters.map { it.ordinal }).containsExactly(1, 2)
        assertThat(twoArgs.parameters.map { it.isDefaultPresent }).containsExactly(false, true)
        assertThat(place.declarations[2].parameters).hasSize(1)

        val total = snapshot.findPackageSubprograms(n("shop"), id("total")).declarations
            .first { it.role == ProjectDeclarationRole.SPECIFICATION }
        assertThat(total).isInstanceOf(ProjectFunctionView::class.java)
        total as ProjectFunctionView
        assertThat(total.isDeterministic).isTrue()
        assertThat(total.returnType.name).isEqualTo(n("money_t"))

        val audit = snapshot.findPackageSubprograms(n("shop"), id("audit")).declarations.single()
        assertThat(audit).isNotInstanceOf(ProjectFunctionView::class.java)
        assertThat(audit.parameters.single().mode).isEqualTo(ProjectParameterMode.IN_OUT)
        assertThat(audit.parameters.single().isNocopy).isTrue()
        assertThat(snapshot.findPackageSubprograms(n("shop"), id("missing")).declarations).isEmpty()
    }

    @Test
    fun correlatesBodyOccurrencesFromTheSameSnapshotOnly() {
        val snapshot = project()
        val placeBody = bodyId(snapshot, "place")
        val correlation = snapshot.correlateSpecification(placeBody)
        assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(correlation.body.id).isEqualTo(placeBody)
        val spec = correlation.specification.orElseThrow()
        assertThat(spec.role).isEqualTo(ProjectDeclarationRole.SPECIFICATION)
        assertThat(correlation.candidates.map { it.id }).containsExactly(spec.id)
        assertThat(snapshot.correlateSpecification(bodyId(snapshot, "hidden")).status)
            .isEqualTo(ProjectSpecificationCorrelation.Status.NOT_FOUND)

        val other = project()
        assertThatThrownBy { other.correlateSpecification(placeBody) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { snapshot.correlateSpecification(spec.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun reportsAmbiguityAndIncompleteEvidence() {
        val ambiguous = project(counted("db/shop_duplicate_type.sql", "shop_duplicate_type.sql"))
            .resolveNamedType(n("order_t"), n("shop"))
        assertThat(ambiguous.status).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS)
        assertThat(ambiguous.declaration).isEmpty
        assertThat(ambiguous.candidates.map { it.id }).doesNotHaveDuplicates().hasSize(2)

        val snapshot = project(failing("db/broken.sql"))
        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES)
        assertThat(snapshot.failures.single().fileId).isEqualTo("db/broken.sql")
        assertThat(snapshot.failures.single().exceptionType).isEqualTo(IOException::class.java.name)
        val known = snapshot.resolveNamedType(n("order_t"), n("shop"))
        assertThat(known.status).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX)
        assertThat(known.candidates).hasSize(1)
        assertThat(snapshot.resolveNamedType(n("nothing"), n("shop")).status)
            .isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX)
        assertThat(snapshot.correlateSpecification(bodyId(snapshot, "place")).status)
            .isEqualTo(ProjectSpecificationCorrelation.Status.INCOMPLETE)
    }

    @Test
    fun retainedSnapshotsAreImmutableAndQueriesNeverReread() {
        val first = project()
        val declarations = first.declarations
        val id = declarations.first().id
        val second = project(counted("db/shop_duplicate_type.sql", "shop_duplicate_type.sql"))
        assertThat(first.declarations).hasSameSizeAs(declarations)
        assertThat(second.declarations.first().id).isNotEqualTo(id)
        val before = reads.get()
        first.resolveNamedType(n("order_t"), n("shop"))
        first.findPackageSubprograms(n("shop"), id("place"))
        first.correlateSpecification(bodyId(first, "place"))
        assertThat(reads.get()).isEqualTo(before)

        val keyed = HashMap<ProjectDeclarationId, ProjectDeclarationKind>()
        first.declarations.forEach { keyed[it.id] = it.kind }
        second.declarations.forEach { keyed[it.id] = it.kind }
        assertThat(keyed).hasSize(first.declarations.size + second.declarations.size)

        @Suppress("UNCHECKED_CAST")
        val mutable = declarations as MutableList<Any?>
        assertThatThrownBy { mutable.clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { mutable.iterator().apply { next() }.remove() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }
}
