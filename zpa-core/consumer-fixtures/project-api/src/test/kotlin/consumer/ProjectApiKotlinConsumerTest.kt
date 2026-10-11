package consumer

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.api.project.*
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

    private fun standalone(): ProjectSnapshot = ProjectSnapshots.prepare(listOf(counted("db/standalone.sql", "standalone.sql")), false)

    @Test
    fun standaloneCallablesExposeCallableFacetsWithoutPackageMembership() {
        val snapshot = standalone()
        assertThat(snapshot.preparationState).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS)
        assertThat(snapshot.declarations.map { it.kind }).containsExactly(
            ProjectDeclarationKind.STANDALONE_PROCEDURE, ProjectDeclarationKind.STANDALONE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.STANDALONE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_PROCEDURE)
        snapshot.declarations.forEach {
            assertThat(it is ProjectSubprogramView).isTrue()
            assertThat(it is ProjectPackageSubprogramView).isFalse()
            assertThat(it.role).isEqualTo(ProjectDeclarationRole.STANDALONE)
        }
        val sync = snapshot.findDeclarations(n("app", "sync_orders")).declarations.first() as ProjectSubprogramView
        assertThat(sync is ProjectFunctionView).isFalse()
        assertThat(sync.localName).isEqualTo(id("sync_orders"))
        assertThat(sync.qualifiedName).isEqualTo(n("app", "sync_orders"))
        assertThat(sync.parameters.map { it.name.originalSpelling }).containsExactly("id", "note", "level_", "col", "total")
        assertThat(sync.parameters.map { it.ordinal }).containsExactly(1, 2, 3, 4, 5)
        assertThat(sync.parameters.map { it.mode }).containsExactly(ProjectParameterMode.IN, ProjectParameterMode.IN_OUT,
            ProjectParameterMode.IN, ProjectParameterMode.IN, ProjectParameterMode.OUT)
        assertThat(sync.parameters.map { it.isNocopy }).containsExactly(false, true, false, false, false)
        assertThat(sync.parameters.map { it.isDefaultPresent }).containsExactly(false, false, true, false, false)
        assertThat(sync.parameters[3].typeRef.anchor.orElseThrow()).isEqualTo(ProjectTypeReference.Anchor.TYPE)
        assertThat(sync.parameters[0].sourceRange.startLine).isEqualTo(2)

        val tax = snapshot.findDeclarations(n("tax")).declarations.first()
        assertThat(tax is ProjectPackageSubprogramView).isFalse()
        tax as ProjectFunctionView
        assertThat(tax.isDeterministic).isTrue()
        assertThat(tax.returnType.name).isEqualTo(n("NUMBER"))
        assertThat((snapshot.findDeclarations(n("now_text")).declarations.first() as ProjectFunctionView).isDeterministic).isFalse()

        val quoted = snapshot.findDeclarations(n("\"Audit\"", "\"Log Event\"")).declarations.first() as ProjectSubprogramView
        assertThat(quoted.localName.originalSpelling).isEqualTo("\"Log Event\"")
        assertThat(quoted.localName.isQuoted).isTrue()
        assertThat(snapshot.findDeclarations(n("Log Event")).declarations).isEmpty()

        assertThat(snapshot.declarationsFor("db/standalone.sql").declarations.map { it.id })
            .isEqualTo(snapshot.declarations.map { it.id })
        assertThat(snapshot.findDeclarations(n("tax")).declarations.first().id).isEqualTo(tax.id)
        @Suppress("UNCHECKED_CAST")
        assertThatThrownBy { (tax.parameters as MutableList<Any?>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThatThrownBy { snapshot.correlateSpecification(tax.id) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { snapshot.correlateSpecification(sync.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun standaloneFunctionAndPackageMemberWithTheSameAuthoredNameStayDistinct() {
        val snapshot = ProjectSnapshots.prepare(listOf(
            counted("db/standalone.sql", "standalone.sql"), counted("db/app_package.sql", "app_package.sql"),
            counted("db/app_package_body.sql", "app_package_body.sql")), false)
        val both = snapshot.findDeclarations(n("app", "f")).declarations
        assertThat(both.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_FUNCTION,
            ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.STANDALONE_FUNCTION)
        assertThat(both.map { it.id }).doesNotHaveDuplicates()
        val standalone = both[2]
        assertThat(standalone is ProjectFunctionView).isTrue()
        assertThat(standalone is ProjectPackageSubprogramView).isFalse()
        both.take(2).forEach {
            assertThat((it as ProjectPackageSubprogramView).packageOwner).isEqualTo(n("app"))
        }
        val members = snapshot.findPackageSubprograms(n("app"), id("f"))
        assertThat(members.declarations.map { it.id }).hasSize(2).doesNotContain(standalone.id)
        val bodyId = members.declarations.single { it.role == ProjectDeclarationRole.BODY }.id
        val correlation = snapshot.correlateSpecification(bodyId)
        assertThat(correlation.status).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED)
        assertThat(correlation.candidates.map { it.id }).doesNotContain(standalone.id)
        assertThatThrownBy { snapshot.correlateSpecification(standalone.id) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    // ---- Package variables and constants ----

    private fun config(): ProjectSnapshot = ProjectSnapshots.prepare(
        listOf(
            counted("db/config_spec.sql", "config_spec.sql"), counted("db/config_duplicate.sql", "config_duplicate.sql"),
            counted("db/shop_spec.sql", "shop_spec.sql"), counted("db/shop_body.sql", "shop_body.sql")
        ), false
    )

    private fun ProjectSnapshot.valuesIn(file: String): List<ProjectPackageValueView> =
        declarationsFor(file).declarations.filterIsInstance<ProjectPackageValueView>()

    private fun ProjectSnapshot.value(file: String, localName: String): ProjectPackageValueView =
        valuesIn(file).single { it.localName == id(localName) }

    @Test
    fun exposesVariablesAndConstantsWithTheirDeclaredTypes() {
        val snapshot = config()
        val values = snapshot.valuesIn("db/config_spec.sql")
        assertThat(values.map { it.localName.originalSpelling }).containsExactly(
            "g", "c", "\"Mixed Case\"", "anchor_source", "anchor_copy", "row_copy", "external_value", "ref_value"
        )
        val g = snapshot.value("db/config_spec.sql", "g")
        val c = snapshot.value("db/config_spec.sql", "c")
        assertThat(g.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE)
        assertThat(c.kind).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT)
        assertThat(g.isConstant).isFalse()
        assertThat(c.isConstant).isTrue()
        assertThat(values.map { it.role }).containsOnly(ProjectDeclarationRole.SPECIFICATION)
        assertThat(values.map { it.owner }).containsOnly(n("app", "config"))
        assertThat(g.qualifiedName).isEqualTo(n("app", "config", "g"))
        assertThat(g.sourceRange.fileId).isEqualTo("db/config_spec.sql")
        assertThat(g.sourceRange.startLine).isEqualTo(2)
        assertThat(g.typeRef.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(g.typeRef.name).isEqualTo(n("NUMBER"))
        assertThat(c.typeRef.name).isEqualTo(n("VARCHAR2"))
        assertThat(c.typeRef.sourceRange.startLine).isEqualTo(3)
    }

    @Test
    fun quotedNamesAndUnresolvedTypeSyntax() {
        val snapshot = config()
        val mixed = snapshot.value("db/config_spec.sql", "\"Mixed Case\"")
        assertThat(mixed.localName.isQuoted).isTrue()
        assertThat(mixed.localName).isNotEqualTo(id("mixed case"))
        assertThat(snapshot.findDeclarations(n("app", "config", "mixed_case")).declarations).isEmpty()

        val external = snapshot.value("db/config_spec.sql", "external_value").typeRef
        assertThat(external.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
        assertThat(external.name).isEqualTo(n("ext_schema", "external_type"))
        assertThat(external.anchor).isEmpty
        val anchored = snapshot.value("db/config_spec.sql", "anchor_copy").typeRef
        assertThat(anchored.kind).isEqualTo(ProjectTypeReference.Kind.ANCHORED)
        assertThat(anchored.anchor).contains(ProjectTypeReference.Anchor.TYPE)
        assertThat(anchored.name).isEqualTo(n("anchor_source"))
        val row = snapshot.value("db/config_spec.sql", "row_copy").typeRef
        assertThat(row.anchor).contains(ProjectTypeReference.Anchor.ROWTYPE)
        assertThat(row.name).isEqualTo(n("orders"))
        val ref = snapshot.value("db/config_spec.sql", "ref_value").typeRef
        assertThat(ref.kind).isEqualTo(ProjectTypeReference.Kind.REF)
        assertThat(ref.name).isEqualTo(n("ext_schema", "obj_t"))
        assertThat(snapshot.resolveNamedType(n("anchor_source"), n("app", "config")).status)
            .isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun inventoryPathsShareIdsAndDuplicatesStayDistinct() {
        val snapshot = config()
        val byName = snapshot.findDeclarations(n("app", "config", "g")).declarations
        val values = byName.filterIsInstance<ProjectPackageValueView>()
        assertThat(values.map { it.fileId }).containsExactlyInAnyOrder("db/config_spec.sql", "db/config_duplicate.sql")
        assertThat(values.map { it.id }).doesNotHaveDuplicates()
        values.forEach { found ->
            assertThat(snapshot.value(found.fileId, "g").id).isEqualTo(found.id)
            assertThat(snapshot.declarations.map { it.id }).contains(found.id)
        }
        assertThat(byName).hasSize(3)
        assertThat(snapshot.findDeclarations(n("app", "config")).declarations.map { it.kind })
            .containsExactly(ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE)
        assertThat(snapshot.findDeclarations(n("app", "config", "missing")).declarations).isEmpty()
        // The Java and Kotlin consumers observe the same declared types.
        assertThat(snapshot.value("db/config_duplicate.sql", "g").typeRef.name).isEqualTo(n("VARCHAR2"))
    }

    @Test
    fun valuesAreNeitherTypesNorSubprograms() {
        val snapshot = config()
        val g = snapshot.value("db/config_spec.sql", "g")
        assertThat(g is ProjectTypeDeclarationView).isFalse()
        assertThat(g is ProjectSubprogramView).isFalse()
        assertThat(g is ProjectPackageSubprogramView).isFalse()
        val candidates = snapshot.findPackageSubprograms(n("app", "config"), id("g")).declarations
        assertThat(candidates.map { it.kind }).containsExactly(ProjectDeclarationKind.PACKAGE_PROCEDURE)
        assertThat(candidates.single().id).isNotEqualTo(g.id)
        val type = snapshot.resolveNamedType(n("rec_t"), n("app", "config"))
        assertThat(type.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        assertThat(type.declaration.get()).isInstanceOf(ProjectPackageTypeView::class.java)
        assertThatThrownBy { snapshot.correlateSpecification(g.id) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(snapshot.findPackageSubprograms(n("shop"), id("place")).declarations).hasSize(3)
        assertThatThrownBy { (snapshot.declarations as MutableList<*>).clear() }
            .isInstanceOf(UnsupportedOperationException::class.java)
    }

    private fun describe(kind: ProjectDeclarationKind): String = when (kind) {
        // Deliberately exhaustive and without an else branch: this stops compiling when the enum grows.
        ProjectDeclarationKind.PACKAGE -> "package"
        ProjectDeclarationKind.PACKAGE_BODY -> "package body"
        ProjectDeclarationKind.STANDALONE_TYPE -> "standalone type"
        ProjectDeclarationKind.PACKAGE_TYPE -> "package type"
        ProjectDeclarationKind.PACKAGE_SUBTYPE -> "package subtype"
        ProjectDeclarationKind.PACKAGE_PROCEDURE -> "package procedure"
        ProjectDeclarationKind.PACKAGE_FUNCTION -> "package function"
        ProjectDeclarationKind.SEQUENCE -> "sequence"
        ProjectDeclarationKind.STANDALONE_PROCEDURE -> "standalone procedure"
        ProjectDeclarationKind.STANDALONE_FUNCTION -> "standalone function"
        ProjectDeclarationKind.PACKAGE_VARIABLE -> "package variable"
        ProjectDeclarationKind.PACKAGE_CONSTANT -> "package constant"
    }

    @Test
    fun declarationKindEnumHasTheDocumentedConstantsInOrder() {
        assertThat(ProjectDeclarationKind.entries.map { it.name }).containsExactly(
            "PACKAGE", "PACKAGE_BODY", "STANDALONE_TYPE", "PACKAGE_TYPE", "PACKAGE_SUBTYPE", "PACKAGE_PROCEDURE",
            "PACKAGE_FUNCTION", "SEQUENCE", "STANDALONE_PROCEDURE", "STANDALONE_FUNCTION", "PACKAGE_VARIABLE", "PACKAGE_CONSTANT"
        )
        ProjectDeclarationKind.entries.forEach { assertThat(describe(it)).isNotBlank() }
    }

    // ---- Nested records, collection elements and bounded type descriptions ----

    private fun types(vararg inputs: ProjectSourceInput): ProjectSnapshot = ProjectSnapshots.prepare(inputs.toList(), false)

    private fun declarationId(snapshot: ProjectSnapshot, vararg qualified: String): ProjectDeclarationId =
        snapshot.findDeclarations(n(*qualified)).declarations.single().id

    // Deliberately exhaustive and without an else branch: this stops compiling when an enum grows.
    private fun label(status: ProjectTypeDescription.Status): String = when (status) {
        ProjectTypeDescription.Status.DECLARED_TYPE -> "declared"
        ProjectTypeDescription.Status.BUILT_IN -> "built-in"
        ProjectTypeDescription.Status.UNRESOLVED_NAME -> "unresolved"
        ProjectTypeDescription.Status.ANCHORED_REFERENCE -> "anchored"
        ProjectTypeDescription.Status.REF_REFERENCE -> "ref"
        ProjectTypeDescription.Status.CYCLE -> "cycle"
    }

    private fun label(category: ProjectBuiltInTypeCategory): String = when (category) {
        ProjectBuiltInTypeCategory.NUMERIC -> "numeric"
        ProjectBuiltInTypeCategory.CHARACTER -> "character"
        ProjectBuiltInTypeCategory.LOB -> "lob"
        ProjectBuiltInTypeCategory.BOOLEAN -> "boolean"
        ProjectBuiltInTypeCategory.DATE -> "date"
        ProjectBuiltInTypeCategory.JSON -> "json"
    }

    @Test
    fun nestedRecordsAreDescribedOneLevelAtATime() {
        val snapshot = types(counted("db/types_nested.sql", "types_nested.sql"))
        val p = n("p")
        val outerResolution = snapshot.resolveNamedType(n("outer_t"), p)
        assertThat(outerResolution.status).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED)
        val outer = outerResolution.declaration.get() as ProjectPackageTypeView
        assertThat(outer.shape.get()).isEqualTo(ProjectTypeShape.RECORD)
        val child = outer.recordFields.single()
        assertThat(child.name.originalSpelling).isEqualTo("child")
        assertThat(child.typeRef.name).isEqualTo(n("inner_t"))

        val inner = snapshot.describeType(child.typeRef, outer.owner)
        assertThat(label(inner.status)).isEqualTo("declared")
        assertThat(inner.declaration.get().id).isEqualTo(declarationId(snapshot, "p", "inner_t"))
        val innerType = inner.declaration.get() as ProjectPackageTypeView
        assertThat(innerType.shape.get()).isEqualTo(ProjectTypeShape.RECORD)
        assertThat(inner.terminalReference.get().sourceRange).isEqualTo(child.typeRef.sourceRange)

        val field = innerType.recordFields.single()
        assertThat(field.typeRef.name).isEqualTo(n("amount"))
        val amount = snapshot.describeType(field.typeRef, innerType.owner)
        assertThat(label(amount.status)).isEqualTo("built-in")
        assertThat(label(amount.builtInCategory.get())).isEqualTo("numeric")
        assertThat(amount.subtypes.map { it.qualifiedName }).containsExactly(n("p", "amount"), n("p", "money"))
        assertThat(amount.subtypes.map { it.id }).containsExactly(declarationId(snapshot, "p", "amount"), declarationId(snapshot, "p", "money"))
        assertThatThrownBy { (amount.subtypes as MutableList<*>).clear() }.isInstanceOf(UnsupportedOperationException::class.java)
        assertThat(snapshot.describeType(n("amount"), p).subtypes.map { it.id }).isEqualTo(amount.subtypes.map { it.id })
        assertThat(snapshot.describeType(n("NUMBER"), p).terminalReference).isEmpty
    }

    @Test
    fun collectionElementsLeadToRecordDeclarationsAndFields() {
        val snapshot = types(counted("db/types_collections.sql", "types_collections.sql"))
        val p = n("p")
        listOf("rows_t", "small_rows_t").forEach { collection ->
            val view = snapshot.resolveNamedType(n(collection), p).declaration.get() as ProjectShapedTypeView
            assertThat(view.shape.get()).isEqualTo(ProjectTypeShape.COLLECTION)
            val element = view.elementType.get()
            assertThat(element.kind).isEqualTo(ProjectTypeReference.Kind.NAMED)
            assertThat(element.name).isEqualTo(n("rec_t"))
            val described = snapshot.describeType(element, p)
            assertThat(label(described.status)).isEqualTo("declared")
            val record = described.declaration.get() as ProjectPackageTypeView
            assertThat(record.qualifiedName).isEqualTo(n("p", "rec_t"))
            assertThat(record.shape.get()).isEqualTo(ProjectTypeShape.RECORD)
            assertThat(record.id).isEqualTo(declarationId(snapshot, "p", "rec_t"))
            assertThat(record.recordFields.map { it.name.originalSpelling }).containsExactly("id", "label")
            assertThat(snapshot.describeType(record.recordFields[0].typeRef, p).builtInCategory.get()).isEqualTo(ProjectBuiltInTypeCategory.NUMERIC)
            assertThat(snapshot.describeType(record.recordFields[1].typeRef, p).builtInCategory.get()).isEqualTo(ProjectBuiltInTypeCategory.CHARACTER)
        }
        assertThat((snapshot.resolveNamedType(n("rec_t"), p).declaration.get() as ProjectShapedTypeView).elementType).isEmpty

        val ids = snapshot.resolveNamedType(n("ids")).declaration.get() as ProjectShapedTypeView
        assertThat(snapshot.describeType(ids.elementType.get(), null).builtInCategory.get()).isEqualTo(ProjectBuiltInTypeCategory.NUMERIC)
        val foreign = snapshot.resolveNamedType(n("foreign_rows")).declaration.get() as ProjectShapedTypeView
        val external = snapshot.describeType(foreign.elementType.get(), null)
        assertThat(label(external.status)).isEqualTo("unresolved")
        assertThat(external.resolution.get().status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
    }

    @Test
    fun descriptionsStopConservatively() {
        val snapshot = types(counted("db/types_nested.sql", "types_nested.sql"))
        val p = n("p")
        val fields = (snapshot.resolveNamedType(n("stop_t"), p).declaration.get() as ProjectPackageTypeView).recordFields
        val external = snapshot.describeType(fields[0].typeRef, p)
        assertThat(external.status).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME)
        assertThat(external.resolution.get().status).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT)
        assertThat(snapshot.describeType(fields[1].typeRef, p).status).isEqualTo(ProjectTypeDescription.Status.ANCHORED_REFERENCE)
        assertThat(snapshot.describeType(fields[2].typeRef, p).terminalReference.get().anchor.get()).isEqualTo(ProjectTypeReference.Anchor.ROWTYPE)
        assertThat(snapshot.describeType(fields[3].typeRef, p).status).isEqualTo(ProjectTypeDescription.Status.REF_REFERENCE)
        val cycle = snapshot.describeType(n("c1"), p)
        assertThat(label(cycle.status)).isEqualTo("cycle")
        assertThat(cycle.subtypes.map { it.localName.originalSpelling }).containsExactly("c1", "c2")
    }

    @Test
    fun duplicatesAndFailedSourcesAreReportedThroughTheNamedResolution() {
        val duplicate = types(counted("db/a.sql", "types_nested.sql"), counted("db/b.sql", "types_nested.sql"))
        val ambiguous = duplicate.describeType(n("amount"), n("p"))
        assertThat(ambiguous.status).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME)
        assertThat(ambiguous.resolution.get().status).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS)
        assertThat(ambiguous.resolution.get().candidates).hasSize(2)
        assertThat(ambiguous.subtypes).isEmpty()

        val failed = types(counted("db/types_nested.sql", "types_nested.sql"), failing("db/broken.sql"))
        val incomplete = failed.describeType(n("amount"), n("p"))
        assertThat(incomplete.resolution.get().status).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX)
        assertThat(incomplete.resolution.get().failures).hasSize(1)
        assertThat(failed.describeType(n("NUMBER"), n("p")).builtInCategory.get()).isEqualTo(ProjectBuiltInTypeCategory.NUMERIC)
    }
}
