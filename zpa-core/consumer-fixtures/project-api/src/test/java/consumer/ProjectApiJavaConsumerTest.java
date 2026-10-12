package consumer;

import com.felipebz.zpa.api.project.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses only the supported com.felipebz.zpa.api.project contract, compiled against the zpa-core jar. */
class ProjectApiJavaConsumerTest {
    private static final AtomicInteger READS = new AtomicInteger();

    private static String sql(String name) {
        try (InputStream in = ProjectApiJavaConsumerTest.class.getResourceAsStream("/sql/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ProjectSourceInput counted(String id, String resource) {
        return ProjectSourceInput.ofReader(id, () -> {
            READS.incrementAndGet();
            return sql(resource);
        });
    }

    private static ProjectSourceInput failing(String id) {
        // A checked exception must be accepted by the reader contract.
        return ProjectSourceInput.ofReader(id, () -> {
            throw new IOException("cannot read " + id);
        });
    }

    private static ProjectSnapshot project(ProjectSourceInput... extra) {
        List<ProjectSourceInput> inputs = new ArrayList<>(List.of(
            counted("db/shop_spec.sql", "shop_spec.sql"), counted("db/shop_body.sql", "shop_body.sql")));
        inputs.addAll(List.of(extra));
        return ProjectSnapshots.prepare(inputs, false);
    }

    private static ProjectQualifiedName name(String... segments) {
        return ProjectQualifiedName.of(segments);
    }

    @Test
    void preparesAndInspectsDeclarations() {
        ProjectSnapshot snapshot = project();
        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
        assertThat(snapshot.getAttemptedFileCount()).isEqualTo(2);
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(2);
        assertThat(snapshot.getFileIds()).containsExactly("db/shop_body.sql", "db/shop_spec.sql");
        assertThat(snapshot.getFailures()).isEmpty();

        Map<ProjectDeclarationKind, Integer> kinds = new EnumMap<>(ProjectDeclarationKind.class);
        for (ProjectDeclarationView view : snapshot.getDeclarations()) {
            kinds.merge(view.getKind(), 1, Integer::sum);
        }
        assertThat(kinds).containsEntry(ProjectDeclarationKind.PACKAGE, 1);
        assertThat(kinds).containsEntry(ProjectDeclarationKind.PACKAGE_TYPE, 1);
        assertThat(kinds).containsEntry(ProjectDeclarationKind.PACKAGE_SUBTYPE, 1);
        assertThat(kinds).containsEntry(ProjectDeclarationKind.PACKAGE_PROCEDURE, 5);
        assertThat(kinds).containsEntry(ProjectDeclarationKind.PACKAGE_FUNCTION, 2);
        assertThat(snapshot.declarationsFor("db/shop_spec.sql").getDeclarations()).hasSize(7);
        assertThat(snapshot.declarationsFor("unknown.sql").getDeclarations()).isEmpty();
    }

    @Test
    void resolvesRecordTypeAndInspectsOrderedFieldsAndSubtype() {
        ProjectSnapshot snapshot = project();
        ProjectNamedTypeResolution resolution = snapshot.resolveNamedType(name("order_t"), name("shop"));
        assertThat(resolution.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED);
        ProjectPackageTypeView type = (ProjectPackageTypeView) resolution.getDeclaration().orElseThrow();
        assertThat(type.getShape()).contains(ProjectTypeShape.RECORD);
        assertThat(type.getOwner()).isEqualTo(name("shop"));
        List<ProjectRecordFieldView> fields = type.getRecordFields();
        assertThat(fields).extracting(f -> f.getName().getOriginalSpelling())
            .containsExactly("id", "customer", "total", "note");
        assertThat(fields).extracting(ProjectRecordFieldView::getOrdinal).containsExactly(0, 1, 2, 3);
        ProjectTypeReference note = fields.get(3).getTypeRef();
        assertThat(note.getKind()).isEqualTo(ProjectTypeReference.Kind.ANCHORED);
        assertThat(note.getAnchor()).contains(ProjectTypeReference.Anchor.TYPE);
        assertThat(note.getName()).isEqualTo(name("orders", "note"));
        assertThat(fields.get(0).getTypeRef().getAnchor()).isEmpty();

        // Qualified names override the context; the same occurrence has the same ID in every query path.
        ProjectNamedTypeResolution qualified = snapshot.resolveNamedType(name("shop", "order_t"));
        assertThat(qualified.getDeclaration().orElseThrow().getId()).isEqualTo(type.getId());
        assertThat(snapshot.findDeclarations(name("shop", "order_t")).getDeclarations().get(0).getId()).isEqualTo(type.getId());
        // No fallback: without package context a one-segment name is not a package member.
        assertThat(snapshot.resolveNamedType(name("order_t")).getStatus())
            .isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);

        ProjectSubtypeView money = (ProjectSubtypeView) snapshot.resolveNamedType(name("money_t"), name("shop"))
            .getDeclaration().orElseThrow();
        assertThat(money.getBaseType().getKind()).isEqualTo(ProjectTypeReference.Kind.NAMED);
        assertThat(money.getBaseType().getName().getLast().getOriginalSpelling()).isEqualTo("NUMBER");
    }

    @Test
    void inspectsOverloadCandidatesAndSignaturesWithoutRanking() {
        ProjectSnapshot snapshot = project();
        ProjectInventory<ProjectPackageSubprogramView> place =
            snapshot.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("place"));
        assertThat(place.getStatus()).isEqualTo(ProjectInventory.Status.COMPLETE);
        assertThat(place.getDeclarations()).hasSize(3) // two specifications and one body
            .extracting(ProjectPackageSubprogramView::getRole)
            .containsExactly(ProjectDeclarationRole.BODY, ProjectDeclarationRole.SPECIFICATION,
                ProjectDeclarationRole.SPECIFICATION);
        ProjectPackageSubprogramView twoArgs = place.getDeclarations().get(1);
        assertThat(twoArgs.getPackageOwner()).isEqualTo(name("shop"));
        List<ProjectParameterView> parameters = twoArgs.getParameters();
        assertThat(parameters).extracting(p -> p.getName().getOriginalSpelling()).containsExactly("o", "notify");
        assertThat(parameters).extracting(ProjectParameterView::getOrdinal).containsExactly(1, 2);
        assertThat(parameters.get(0).getMode()).isEqualTo(ProjectParameterMode.IN);
        assertThat(parameters.get(0).getTypeRef().getName()).isEqualTo(name("order_t"));
        assertThat(parameters.get(1).isDefaultPresent()).isTrue();
        assertThat(parameters.get(0).isDefaultPresent()).isFalse();
        assertThat(place.getDeclarations().get(2).getParameters()).hasSize(1);

        ProjectPackageSubprogramView total = snapshot
            .findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("total")).getDeclarations().stream()
            .filter(d -> d.getRole() == ProjectDeclarationRole.SPECIFICATION).findFirst().orElseThrow();
        assertThat(total).isInstanceOf(ProjectFunctionView.class);
        ProjectFunctionView function = (ProjectFunctionView) total;
        assertThat(function.isDeterministic()).isTrue();
        assertThat(function.getReturnType().getName()).isEqualTo(name("money_t"));

        ProjectPackageSubprogramView audit = snapshot
            .findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("audit")).getDeclarations().get(0);
        assertThat(audit).isNotInstanceOf(ProjectFunctionView.class);
        assertThat(audit.getParameters().get(0).getMode()).isEqualTo(ProjectParameterMode.IN_OUT);
        assertThat(audit.getParameters().get(0).isNocopy()).isTrue();

        assertThat(snapshot.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("missing"))
            .getDeclarations()).isEmpty();
    }

    @Test
    void correlatesBodyOccurrencesFromTheSameSnapshotOnly() {
        ProjectSnapshot snapshot = project();
        ProjectDeclarationId placeBody = bodyId(snapshot, "place");
        ProjectSpecificationCorrelation correlation = snapshot.correlateSpecification(placeBody);
        assertThat(correlation.getStatus()).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED);
        assertThat(correlation.getBody().getId()).isEqualTo(placeBody);
        ProjectPackageSubprogramView spec = correlation.getSpecification().orElseThrow();
        assertThat(spec.getRole()).isEqualTo(ProjectDeclarationRole.SPECIFICATION);
        assertThat(spec.getParameters()).hasSize(2);
        assertThat(correlation.getCandidates()).extracting(ProjectPackageSubprogramView::getId).containsExactly(spec.getId());

        // A body without a supplied specification is not found; that does not make it private.
        assertThat(snapshot.correlateSpecification(bodyId(snapshot, "hidden")).getStatus())
            .isEqualTo(ProjectSpecificationCorrelation.Status.NOT_FOUND);

        ProjectSnapshot other = project();
        assertThatThrownBy(() -> other.correlateSpecification(placeBody)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.correlateSpecification(spec.getId())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.correlateSpecification(null)).isInstanceOf(NullPointerException.class);
    }

    private static ProjectDeclarationId bodyId(ProjectSnapshot snapshot, String name) {
        return snapshot.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource(name)).getDeclarations()
            .stream().filter(d -> d.getRole() == ProjectDeclarationRole.BODY).findFirst().orElseThrow().getId();
    }

    @Test
    void reportsAmbiguityForDuplicateTypeDeclarations() {
        ProjectSnapshot snapshot = project(counted("db/shop_duplicate_type.sql", "shop_duplicate_type.sql"));
        ProjectNamedTypeResolution resolution = snapshot.resolveNamedType(name("order_t"), name("shop"));
        assertThat(resolution.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS);
        assertThat(resolution.getDeclaration()).isEmpty();
        assertThat(resolution.getCandidates()).hasSize(2);
        assertThat(resolution.getCandidates().get(0).getId()).isNotEqualTo(resolution.getCandidates().get(1).getId());
        // Specifications still correlate: the duplicate only declares a type.
        assertThat(snapshot.correlateSpecification(bodyId(snapshot, "place")).getStatus())
            .isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED);
    }

    @Test
    void reportsIncompleteEvidenceForFailedSources() {
        ProjectSnapshot snapshot = project(failing("db/broken.sql"));
        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_FAILURES);
        assertThat(snapshot.getFailures()).hasSize(1);
        assertThat(snapshot.getFailures().get(0).getFileId()).isEqualTo("db/broken.sql");
        assertThat(snapshot.getFailures().get(0).getExceptionType()).isEqualTo(IOException.class.getName());
        assertThat(snapshot.getSuccessfulFileCount()).isEqualTo(2);

        ProjectNamedTypeResolution known = snapshot.resolveNamedType(name("order_t"), name("shop"));
        assertThat(known.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX);
        assertThat(known.getDeclaration()).isEmpty();
        assertThat(known.getCandidates()).hasSize(1);
        ProjectNamedTypeResolution unknown = snapshot.resolveNamedType(name("nothing"), name("shop"));
        assertThat(unknown.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX);
        assertThat(unknown.getCandidates()).isEmpty();
        assertThat(snapshot.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("nothing")).getStatus())
            .isEqualTo(ProjectInventory.Status.INCOMPLETE_INDEX);
        assertThat(snapshot.correlateSpecification(bodyId(snapshot, "place")).getStatus())
            .isEqualTo(ProjectSpecificationCorrelation.Status.INCOMPLETE);
    }

    @Test
    void retainedSnapshotsAreImmutableAndQueriesNeverReread() {
        ProjectSnapshot first = project();
        int readsAfterPreparation = READS.get();
        List<ProjectDeclarationView> declarations = first.getDeclarations();
        ProjectDeclarationView view = declarations.get(0);
        ProjectDeclarationId id = view.getId();

        // Later preparations do not affect a retained snapshot or its IDs.
        ProjectSnapshot second = project(counted("db/shop_duplicate_type.sql", "shop_duplicate_type.sql"));
        assertThat(first.getDeclarations()).hasSameSizeAs(declarations);
        assertThat(second.getDeclarations().get(0).getId()).isNotEqualTo(id);
        int before = READS.get();

        first.resolveNamedType(name("order_t"), name("shop"));
        first.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("place"));
        first.correlateSpecification(bodyId(first, "place"));
        first.declarationsFor("db/shop_spec.sql");
        assertThat(READS.get()).isEqualTo(before);
        assertThat(readsAfterPreparation).isPositive();

        // IDs are complete map keys; same coordinates from another snapshot are distinct keys.
        Map<ProjectDeclarationId, ProjectDeclarationKind> byId = new HashMap<>();
        first.getDeclarations().forEach(d -> byId.put(d.getId(), d.getKind()));
        second.getDeclarations().forEach(d -> byId.put(d.getId(), d.getKind()));
        assertThat(byId).hasSize(first.getDeclarations().size() + second.getDeclarations().size());
    }

    @Test
    void publicCollectionsRejectEveryMutation() {
        ProjectSnapshot snapshot = project(failing("db/broken.sql"));
        ProjectPackageTypeView type = (ProjectPackageTypeView) snapshot
            .resolveNamedType(name("order_t"), name("shop")).getCandidates().get(0);
        ProjectPackageSubprogramView place = snapshot
            .findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("place")).getDeclarations().get(1);
        List<?>[] lists = {
            snapshot.getDeclarations(), snapshot.getFileIds(), snapshot.getFailures(),
            snapshot.declarationsFor("db/shop_spec.sql").getDeclarations(),
            snapshot.declarationsFor("db/shop_spec.sql").getFailures(),
            snapshot.resolveNamedType(name("order_t"), name("shop")).getCandidates(),
            snapshot.correlateSpecification(bodyId(snapshot, "place")).getFailures(),
            type.getRecordFields(), place.getParameters(), name("a", "b").getSegments()
        };
        for (List<?> list : lists) {
            assertThatThrownBy(list::clear).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> ((List<Object>) list).add(null)).isInstanceOf(UnsupportedOperationException.class);
            if (!list.isEmpty()) {
                Iterator<?> iterator = list.iterator();
                iterator.next();
                assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
            }
        }
    }

    @Test
    void identifiersCompareByQuotingAndCase() {
        assertThat(ProjectIdentifier.fromSource("Shop")).isEqualTo(ProjectIdentifier.fromSource("SHOP"));
        assertThat(ProjectIdentifier.fromSource("\"Shop\"")).isNotEqualTo(ProjectIdentifier.fromSource("Shop"));
        assertThat(ProjectIdentifier.fromSource("\"Shop\"").isQuoted()).isTrue();
        assertThat(ProjectQualifiedName.of("a.b").getSegments()).hasSize(1);
        assertThat(ProjectQualifiedName.of("\"a.b\"", "c").getSegments()).hasSize(2);
        assertThatThrownBy(() -> ProjectIdentifier.fromSource("")).isInstanceOf(IllegalArgumentException.class);
    }

    private static ProjectSnapshot standalone() {
        return ProjectSnapshots.prepare(List.of(counted("db/standalone.sql", "standalone.sql")), false);
    }

    @Test
    void standaloneCallablesAreSubprogramsWithoutPackageMembership() {
        ProjectSnapshot snapshot = standalone();
        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
        assertThat(snapshot.getDeclarations()).extracting(ProjectDeclarationView::getKind).containsExactly(
            ProjectDeclarationKind.STANDALONE_PROCEDURE, ProjectDeclarationKind.STANDALONE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_FUNCTION, ProjectDeclarationKind.STANDALONE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_PROCEDURE);
        for (ProjectDeclarationView view : snapshot.getDeclarations()) {
            assertThat(view).isInstanceOf(ProjectSubprogramView.class).isNotInstanceOf(ProjectPackageSubprogramView.class);
            assertThat(view.getRole()).isEqualTo(ProjectDeclarationRole.STANDALONE);
        }
    }

    @Test
    void standaloneProcedureSignaturesExposeModesDefaultsAndAnchors() {
        ProjectSnapshot snapshot = standalone();
        ProjectSubprogramView sync = (ProjectSubprogramView) snapshot.findDeclarations(name("app", "sync_orders"))
            .getDeclarations().get(0);
        assertThat(sync).isNotInstanceOf(ProjectFunctionView.class);
        assertThat(sync.getQualifiedName()).isEqualTo(name("app", "sync_orders"));
        assertThat(sync.getLocalName()).isEqualTo(ProjectIdentifier.fromSource("sync_orders"));
        List<ProjectParameterView> parameters = sync.getParameters();
        assertThat(parameters).extracting(p -> p.getName().getOriginalSpelling())
            .containsExactly("id", "note", "level_", "col", "total");
        assertThat(parameters).extracting(ProjectParameterView::getOrdinal).containsExactly(1, 2, 3, 4, 5);
        assertThat(parameters).extracting(ProjectParameterView::getMode).containsExactly(
            ProjectParameterMode.IN, ProjectParameterMode.IN_OUT, ProjectParameterMode.IN,
            ProjectParameterMode.IN, ProjectParameterMode.OUT);
        assertThat(parameters).extracting(ProjectParameterView::isNocopy).containsExactly(false, true, false, false, false);
        assertThat(parameters).extracting(ProjectParameterView::isDefaultPresent).containsExactly(false, false, true, false, false);
        assertThat(parameters.get(3).getTypeRef().getAnchor()).contains(ProjectTypeReference.Anchor.TYPE);
        assertThat(parameters.get(3).getTypeRef().getName()).isEqualTo(name("orders", "note"));
        assertThat(parameters.get(0).getSourceRange().getFileId()).isEqualTo("db/standalone.sql");
        assertThat(parameters.get(0).getSourceRange().getStartLine()).isEqualTo(2);
    }

    @Test
    void standaloneFunctionsExposeReturnTypeAndDeterminism() {
        ProjectSnapshot snapshot = standalone();
        ProjectFunctionView tax = (ProjectFunctionView) snapshot.findDeclarations(name("tax")).getDeclarations().get(0);
        assertThat(tax).isInstanceOf(ProjectSubprogramView.class).isNotInstanceOf(ProjectPackageSubprogramView.class);
        assertThat(tax.isDeterministic()).isTrue();
        assertThat(tax.getReturnType().getName()).isEqualTo(name("NUMBER"));
        assertThat(tax.getParameters()).hasSize(2);
        ProjectFunctionView nowText = (ProjectFunctionView) snapshot.findDeclarations(name("now_text")).getDeclarations().get(0);
        assertThat(nowText.isDeterministic()).isFalse();
        assertThat(nowText.getParameters()).isEmpty();
    }

    @Test
    void quotedStandaloneNamesKeepTheirAuthoredSpelling() {
        ProjectSnapshot snapshot = standalone();
        ProjectSubprogramView quoted = (ProjectSubprogramView) snapshot.findDeclarations(name("\"Audit\"", "\"Log Event\""))
            .getDeclarations().get(0);
        assertThat(quoted.getLocalName().getOriginalSpelling()).isEqualTo("\"Log Event\"");
        assertThat(quoted.getLocalName().isQuoted()).isTrue();
        assertThat(quoted.getQualifiedName().getSegments()).hasSize(2);
        // Authored name only: the first segment is neither a package owner nor an inferred schema.
        assertThat(snapshot.findDeclarations(name("Log Event")).getDeclarations()).isEmpty();
    }

    @Test
    void standaloneIdentitiesAreCanonicalAndCollectionsAreImmutable() {
        ProjectSnapshot snapshot = standalone();
        ProjectSubprogramView sync = (ProjectSubprogramView) snapshot.findDeclarations(name("app", "sync_orders"))
            .getDeclarations().get(0);
        ProjectFunctionView tax = (ProjectFunctionView) snapshot.findDeclarations(name("tax")).getDeclarations().get(0);
        // Canonical IDs across query paths; mutation is rejected.
        assertThat(snapshot.declarationsFor("db/standalone.sql").getDeclarations()).extracting(ProjectDeclarationView::getId)
            .isEqualTo(snapshot.getDeclarations().stream().map(ProjectDeclarationView::getId).toList());
        assertThat(snapshot.findDeclarations(name("tax")).getDeclarations().get(0).getId()).isEqualTo(tax.getId());
        List<ProjectParameterView> taxParameters = tax.getParameters();
        assertThatThrownBy(taxParameters::clear).isInstanceOf(UnsupportedOperationException.class);
        Iterator<ProjectParameterView> iterator = sync.getParameters().iterator();
        iterator.next();
        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        List<ProjectDeclarationView> allDeclarations = snapshot.getDeclarations();
        assertThatThrownBy(allDeclarations::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void standaloneDeclarationsAreNeverCorrelationInputs() {
        ProjectSnapshot snapshot = standalone();
        ProjectSubprogramView sync = (ProjectSubprogramView) snapshot.findDeclarations(name("app", "sync_orders"))
            .getDeclarations().get(0);
        ProjectFunctionView tax = (ProjectFunctionView) snapshot.findDeclarations(name("tax")).getDeclarations().get(0);
        assertThatThrownBy(() -> snapshot.correlateSpecification(tax.getId())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.correlateSpecification(sync.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standaloneFunctionAndPackageMemberWithTheSameAuthoredNameStayDistinct() {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            counted("db/standalone.sql", "standalone.sql"), counted("db/app_package.sql", "app_package.sql"),
            counted("db/app_package_body.sql", "app_package_body.sql")), false);
        List<ProjectDeclarationView> both = snapshot.findDeclarations(name("app", "f")).getDeclarations();
        assertThat(both).extracting(ProjectDeclarationView::getKind).containsExactly(
            ProjectDeclarationKind.PACKAGE_FUNCTION, ProjectDeclarationKind.PACKAGE_FUNCTION,
            ProjectDeclarationKind.STANDALONE_FUNCTION);
        assertThat(both).extracting(ProjectDeclarationView::getId).doesNotHaveDuplicates();
        ProjectDeclarationView standalone = both.get(2);
        assertThat(standalone).isInstanceOf(ProjectFunctionView.class).isNotInstanceOf(ProjectPackageSubprogramView.class);
        assertThat(standalone.getRole()).isEqualTo(ProjectDeclarationRole.STANDALONE);
        for (ProjectDeclarationView member : both.subList(0, 2)) {
            assertThat(member).isInstanceOf(ProjectFunctionView.class).isInstanceOf(ProjectPackageSubprogramView.class);
            assertThat(((ProjectPackageSubprogramView) member).getPackageOwner()).isEqualTo(name("app"));
        }

        ProjectInventory<ProjectPackageSubprogramView> members =
            snapshot.findPackageSubprograms(name("app"), ProjectIdentifier.fromSource("f"));
        assertThat(members.getDeclarations()).extracting(ProjectDeclarationView::getId)
            .doesNotContain(standalone.getId()).hasSize(2);
        assertThat(members.getDeclarations()).extracting(ProjectDeclarationView::getRole)
            .containsExactlyInAnyOrder(ProjectDeclarationRole.BODY, ProjectDeclarationRole.SPECIFICATION);
        // Package-only correlation still works for the body and never mentions the standalone function.
        ProjectDeclarationId bodyId = members.getDeclarations().stream()
            .filter(m -> m.getRole() == ProjectDeclarationRole.BODY).findFirst().orElseThrow().getId();
        ProjectSpecificationCorrelation correlation = snapshot.correlateSpecification(bodyId);
        assertThat(correlation.getStatus()).isEqualTo(ProjectSpecificationCorrelation.Status.RESOLVED);
        assertThat(correlation.getCandidates()).extracting(ProjectDeclarationView::getId).doesNotContain(standalone.getId());
        assertThatThrownBy(() -> snapshot.correlateSpecification(standalone.getId())).isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Package variables and constants ----

    private static ProjectSnapshot config() {
        return ProjectSnapshots.prepare(List.of(
            counted("db/config_spec.sql", "config_spec.sql"), counted("db/config_duplicate.sql", "config_duplicate.sql"),
            counted("db/shop_spec.sql", "shop_spec.sql"), counted("db/shop_body.sql", "shop_body.sql")), false);
    }

    private static List<ProjectPackageValueView> values(ProjectSnapshot snapshot, String file) {
        List<ProjectPackageValueView> result = new ArrayList<>();
        for (ProjectDeclarationView view : snapshot.declarationsFor(file).getDeclarations()) {
            if (view instanceof ProjectPackageValueView value) {
                result.add(value);
            }
        }
        return result;
    }

    private static ProjectPackageValueView value(ProjectSnapshot snapshot, String file, String localName) {
        return values(snapshot, file).stream()
            .filter(v -> v.getLocalName().equals(ProjectIdentifier.fromSource(localName))).findFirst().orElseThrow();
    }

    @Test
    void exposesVariablesAndConstantsWithTheirDeclaredTypes() {
        ProjectSnapshot snapshot = config();
        assertThat(snapshot.getPreparationState()).isEqualTo(ProjectPreparationState.PREPARED_WITH_DECLARATIONS);
        List<ProjectPackageValueView> values = values(snapshot, "db/config_spec.sql");
        assertThat(values).extracting(v -> v.getLocalName().getOriginalSpelling()).containsExactly(
            "g", "c", "\"Mixed Case\"", "anchor_source", "anchor_copy", "row_copy", "external_value", "ref_value");

        ProjectPackageValueView g = value(snapshot, "db/config_spec.sql", "g");
        ProjectPackageValueView c = value(snapshot, "db/config_spec.sql", "c");
        assertThat(g.getKind()).isEqualTo(ProjectDeclarationKind.PACKAGE_VARIABLE);
        assertThat(c.getKind()).isEqualTo(ProjectDeclarationKind.PACKAGE_CONSTANT);
        assertThat(g.isConstant()).isFalse();
        assertThat(c.isConstant()).isTrue();
        assertThat(values).extracting(ProjectPackageValueView::getRole).containsOnly(ProjectDeclarationRole.SPECIFICATION);
        assertThat(values).extracting(ProjectPackageValueView::getOwner).containsOnly(name("app", "config"));
        assertThat(g.getQualifiedName()).isEqualTo(name("app", "config", "g"));
        assertThat(g.getFileId()).isEqualTo("db/config_spec.sql");
        ProjectSourceRange range = g.getSourceRange();
        assertThat(range.getFileId()).isEqualTo("db/config_spec.sql");
        assertThat(range.getStartLine()).isEqualTo(2);
        assertThat(c.getSourceRange().getStartLine()).isEqualTo(3);

        assertThat(g.getTypeRef().getKind()).isEqualTo(ProjectTypeReference.Kind.NAMED);
        assertThat(g.getTypeRef().getName()).isEqualTo(name("NUMBER"));
        assertThat(g.getTypeRef().getAnchor()).isEmpty();
        assertThat(c.getTypeRef().getName()).isEqualTo(name("VARCHAR2"));
        assertThat(c.getTypeRef().getSourceRange().getStartLine()).isEqualTo(3);
    }

    @Test
    void quotedAndUnquotedIdentifiersKeepTheirSemantics() {
        ProjectSnapshot snapshot = config();
        ProjectPackageValueView mixed = value(snapshot, "db/config_spec.sql", "\"Mixed Case\"");
        assertThat(mixed.getLocalName().isQuoted()).isTrue();
        assertThat(mixed.getLocalName()).isNotEqualTo(ProjectIdentifier.fromSource("mixed case"));
        assertThat(snapshot.findDeclarations(name("app", "config", "\"Mixed Case\"")).getDeclarations()).hasSize(1);
        assertThat(snapshot.findDeclarations(name("app", "config", "mixed_case")).getDeclarations()).isEmpty();
        ProjectPackageValueView g = value(snapshot, "db/config_spec.sql", "G");
        assertThat(g.getLocalName().isQuoted()).isFalse();
        assertThat(g.getLocalName()).isEqualTo(ProjectIdentifier.fromSource("g"));
    }

    @Test
    void externalAnchoredAndRefTypesRemainUnresolvedSyntax() {
        ProjectSnapshot snapshot = config();
        ProjectTypeReference external = value(snapshot, "db/config_spec.sql", "external_value").getTypeRef();
        assertThat(external.getKind()).isEqualTo(ProjectTypeReference.Kind.NAMED);
        assertThat(external.getName()).isEqualTo(name("ext_schema", "external_type"));
        assertThat(external.getAnchor()).isEmpty();

        ProjectTypeReference anchored = value(snapshot, "db/config_spec.sql", "anchor_copy").getTypeRef();
        assertThat(anchored.getKind()).isEqualTo(ProjectTypeReference.Kind.ANCHORED);
        assertThat(anchored.getAnchor()).contains(ProjectTypeReference.Anchor.TYPE);
        assertThat(anchored.getName()).isEqualTo(name("anchor_source"));

        ProjectTypeReference row = value(snapshot, "db/config_spec.sql", "row_copy").getTypeRef();
        assertThat(row.getKind()).isEqualTo(ProjectTypeReference.Kind.ANCHORED);
        assertThat(row.getAnchor()).contains(ProjectTypeReference.Anchor.ROWTYPE);
        assertThat(row.getName()).isEqualTo(name("orders"));

        ProjectTypeReference ref = value(snapshot, "db/config_spec.sql", "ref_value").getTypeRef();
        assertThat(ref.getKind()).isEqualTo(ProjectTypeReference.Kind.REF);
        assertThat(ref.getName()).isEqualTo(name("ext_schema", "obj_t"));

        // The anchor is not followed, even though anchor_source is declared in the same specification.
        assertThat(snapshot.resolveNamedType(name("anchor_source"), name("app", "config")).getStatus())
            .isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);
        assertThat(snapshot.resolveNamedType(name("external_type"), name("ext_schema")).getStatus())
            .isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);
    }

    @Test
    void inventoryPathsShareIdsAndDuplicateSpecificationsStayDistinct() {
        ProjectSnapshot snapshot = config();
        ProjectQualifiedName g = name("app", "config", "g");
        List<ProjectPackageValueView> byName = new ArrayList<>();
        for (ProjectDeclarationView view : snapshot.findDeclarations(g).getDeclarations()) {
            if (view instanceof ProjectPackageValueView value) {
                byName.add(value);
            }
        }
        assertThat(byName).extracting(ProjectPackageValueView::getFileId)
            .containsExactlyInAnyOrder("db/config_spec.sql", "db/config_duplicate.sql");
        assertThat(byName).extracting(ProjectPackageValueView::getId).doesNotHaveDuplicates();
        for (ProjectPackageValueView found : byName) {
            ProjectPackageValueView viaFile = value(snapshot, found.getFileId(), "g");
            assertThat(viaFile.getId()).isEqualTo(found.getId());
            assertThat(snapshot.getDeclarations()).extracting(ProjectDeclarationView::getId).contains(found.getId());
        }
        ProjectPackageValueView duplicate = value(snapshot, "db/config_duplicate.sql", "g");
        assertThat(duplicate.getTypeRef().getName()).isEqualTo(name("VARCHAR2"));
        // The same package name is not deduplicated.
        assertThat(snapshot.findDeclarations(name("app", "config")).getDeclarations())
            .extracting(ProjectDeclarationView::getKind)
            .containsExactly(ProjectDeclarationKind.PACKAGE, ProjectDeclarationKind.PACKAGE);
        assertThat(snapshot.findDeclarations(name("app", "config", "missing")).getDeclarations()).isEmpty();
        assertThat(snapshot.findDeclarations(name("app", "config", "missing")).getStatus())
            .isEqualTo(ProjectInventory.Status.COMPLETE);
    }

    @Test
    void valuesDoNotEnterTypeOrSubprogramSurfaces() {
        ProjectSnapshot snapshot = config();
        ProjectPackageValueView g = value(snapshot, "db/config_spec.sql", "g");
        assertThat((Object) g).isNotInstanceOf(ProjectTypeDeclarationView.class)
            .isNotInstanceOf(ProjectSubprogramView.class)
            .isNotInstanceOf(ProjectPackageSubprogramView.class);

        // A procedure with the same textual name is a separate occurrence and the only package subprogram candidate.
        List<ProjectPackageSubprogramView> candidates = snapshot
            .findPackageSubprograms(name("app", "config"), ProjectIdentifier.fromSource("g")).getDeclarations();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getKind()).isEqualTo(ProjectDeclarationKind.PACKAGE_PROCEDURE);
        assertThat(candidates.get(0).getId()).isNotEqualTo(g.getId());
        assertThat(snapshot.findDeclarations(name("app", "config", "g")).getDeclarations()).hasSize(3);

        ProjectNamedTypeResolution type = snapshot.resolveNamedType(name("rec_t"), name("app", "config"));
        assertThat(type.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED);
        assertThat(type.getDeclaration().orElseThrow()).isInstanceOf(ProjectPackageTypeView.class);

        assertThatThrownBy(() -> snapshot.correlateSpecification(g.getId())).isInstanceOf(IllegalArgumentException.class);
        // Existing shop queries are unchanged by the new values.
        assertThat(snapshot.findPackageSubprograms(name("shop"), ProjectIdentifier.fromSource("place")).getDeclarations())
            .hasSize(3);
    }

    @Test
    void valueViewsExposeNoMutationSurface() {
        ProjectSnapshot snapshot = config();
        List<ProjectDeclarationView> fileDeclarations = snapshot.declarationsFor("db/config_spec.sql").getDeclarations();
        assertThatThrownBy(fileDeclarations::clear)
            .isInstanceOf(UnsupportedOperationException.class);
        List<ProjectDeclarationView> valueDeclarations = snapshot.findDeclarations(name("app", "config", "g")).getDeclarations();
        assertThatThrownBy(() -> valueDeclarations.remove(0))
            .isInstanceOf(UnsupportedOperationException.class);
        ProjectPackageValueView g = value(snapshot, "db/config_spec.sql", "g");
        assertThat(ProjectPackageValueView.class.getMethods()).extracting(java.lang.reflect.Method::getName)
            .noneMatch(n -> n.startsWith("set"));
        assertThat(g.getOwner().getSegments()).isUnmodifiable();
        ProjectDeclarationId sameId = value(snapshot, "db/config_spec.sql", "g").getId();
        assertThat(g.getId()).isEqualTo(sameId).hasSameHashCodeAs(sameId);
    }

    private static String describe(ProjectDeclarationKind kind) {
        // Deliberately exhaustive and without a default branch: this stops compiling when the enum grows.
        return switch (kind) {
            case PACKAGE -> "package";
            case PACKAGE_BODY -> "package body";
            case STANDALONE_TYPE -> "standalone type";
            case PACKAGE_TYPE -> "package type";
            case PACKAGE_SUBTYPE -> "package subtype";
            case PACKAGE_PROCEDURE -> "package procedure";
            case PACKAGE_FUNCTION -> "package function";
            case SEQUENCE -> "sequence";
            case STANDALONE_PROCEDURE -> "standalone procedure";
            case STANDALONE_FUNCTION -> "standalone function";
            case PACKAGE_VARIABLE -> "package variable";
            case PACKAGE_CONSTANT -> "package constant";
        };
    }

    @Test
    void declarationKindEnumHasTheDocumentedConstantsInOrder() {
        assertThat(ProjectDeclarationKind.values()).extracting(Enum::name).containsExactly(
            "PACKAGE", "PACKAGE_BODY", "STANDALONE_TYPE", "PACKAGE_TYPE", "PACKAGE_SUBTYPE", "PACKAGE_PROCEDURE",
            "PACKAGE_FUNCTION", "SEQUENCE", "STANDALONE_PROCEDURE", "STANDALONE_FUNCTION", "PACKAGE_VARIABLE",
            "PACKAGE_CONSTANT");
        for (ProjectDeclarationKind kind : ProjectDeclarationKind.values()) {
            assertThat(describe(kind)).isNotBlank();
        }
        assertThat(describe(ProjectDeclarationKind.PACKAGE_VARIABLE)).isEqualTo("package variable");
        assertThat(ProjectDeclarationKind.PACKAGE_VARIABLE.ordinal()).isEqualTo(10);
        assertThat(ProjectDeclarationKind.PACKAGE_CONSTANT.ordinal()).isEqualTo(11);
    }

    // ---- Nested records, collection elements and bounded type descriptions ----

    private static ProjectSnapshot types(ProjectSourceInput... inputs) {
        return ProjectSnapshots.prepare(List.of(inputs), false);
    }

    private static ProjectDeclarationId declarationId(ProjectSnapshot snapshot, String... qualified) {
        List<ProjectDeclarationView> found = snapshot.findDeclarations(name(qualified)).getDeclarations();
        assertThat(found).hasSize(1);
        return found.get(0).getId();
    }

    // Deliberately exhaustive and without a default branch: this stops compiling when an enum grows.
    private static String label(ProjectTypeDescription.Status status) {
        return switch (status) {
            case DECLARED_TYPE -> "declared";
            case BUILT_IN -> "built-in";
            case UNRESOLVED_NAME -> "unresolved";
            case ANCHORED_REFERENCE -> "anchored";
            case REF_REFERENCE -> "ref";
            case CYCLE -> "cycle";
            case UNRESOLVED_VALUE -> "unresolved value";
        };
    }

    private static String label(ProjectBuiltInTypeCategory category) {
        return switch (category) {
            case NUMERIC -> "numeric";
            case CHARACTER -> "character";
            case LOB -> "lob";
            case BOOLEAN -> "boolean";
            case DATE -> "date";
            case JSON -> "json";
        };
    }

    @Test
    void nestedRecordsAreDescribedOneLevelAtATime() {
        ProjectSnapshot snapshot = types(counted("db/types_nested.sql", "types_nested.sql"));
        ProjectQualifiedName p = name("p");

        ProjectNamedTypeResolution outerResolution = snapshot.resolveNamedType(name("outer_t"), p);
        assertThat(outerResolution.getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.RESOLVED);
        ProjectPackageTypeView outer = (ProjectPackageTypeView) outerResolution.getDeclaration().orElseThrow();
        assertThat(outer.getShape()).contains(ProjectTypeShape.RECORD);
        assertThat(outer.getRecordFields()).hasSize(1);
        ProjectRecordFieldView child = outer.getRecordFields().get(0);
        assertThat(child.getName().getOriginalSpelling()).isEqualTo("child");
        assertThat(child.getTypeRef().getName()).isEqualTo(name("inner_t"));

        ProjectTypeDescription inner = snapshot.describeType(child.getTypeRef(), outer.getOwner());
        assertThat(label(inner.getStatus())).isEqualTo("declared");
        assertThat(inner.getDeclaration().orElseThrow().getId()).isEqualTo(declarationId(snapshot, "p", "inner_t"));
        ProjectPackageTypeView innerType = (ProjectPackageTypeView) inner.getDeclaration().orElseThrow();
        assertThat(innerType.getShape()).contains(ProjectTypeShape.RECORD);
        assertThat(inner.getSubtypes()).isEmpty();
        assertThat(inner.getTerminalReference().orElseThrow().getSourceRange()).isEqualTo(child.getTypeRef().getSourceRange());

        ProjectRecordFieldView n = innerType.getRecordFields().get(0);
        assertThat(n.getTypeRef().getName()).isEqualTo(name("amount"));
        ProjectTypeDescription amount = snapshot.describeType(n.getTypeRef(), innerType.getOwner());
        assertThat(label(amount.getStatus())).isEqualTo("built-in");
        assertThat(label(amount.getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
        assertThat(amount.getSubtypes()).extracting(ProjectSubtypeView::getQualifiedName).containsExactly(name("p", "amount"), name("p", "money"));
        assertThat(amount.getSubtypes()).extracting(ProjectSubtypeView::getId)
            .containsExactly(declarationId(snapshot, "p", "amount"), declarationId(snapshot, "p", "money"));
        assertThat(amount.getTerminalReference().orElseThrow().getName()).isEqualTo(name("NUMBER"));
        List<ProjectSubtypeView> amountSubtypes = amount.getSubtypes();
        assertThatThrownBy(amountSubtypes::clear).isInstanceOf(UnsupportedOperationException.class);
        // The name-only overload gives the same alias chain but has no source range for the initial name.
        ProjectTypeDescription byName = snapshot.describeType(name("amount"), p);
        assertThat(byName.getSubtypes()).extracting(ProjectSubtypeView::getId).isEqualTo(
            amount.getSubtypes().stream().map(ProjectSubtypeView::getId).toList());
        assertThat(snapshot.describeType(name("NUMBER"), p).getTerminalReference()).isEmpty();
    }

    @Test
    void collectionElementsLeadToRecordDeclarationsAndFields() {
        ProjectSnapshot snapshot = types(counted("db/types_collections.sql", "types_collections.sql"));
        ProjectQualifiedName p = name("p");
        for (String collection : List.of("rows_t", "small_rows_t")) {
            ProjectShapedTypeView view = (ProjectShapedTypeView) snapshot.resolveNamedType(name(collection), p).getDeclaration().orElseThrow();
            assertThat(view.getShape()).contains(ProjectTypeShape.COLLECTION);
            ProjectTypeReference element = view.getElementType().orElseThrow();
            assertThat(element.getKind()).isEqualTo(ProjectTypeReference.Kind.NAMED);
            assertThat(element.getName()).isEqualTo(name("rec_t"));
            ProjectTypeDescription described = snapshot.describeType(element, p);
            assertThat(label(described.getStatus())).isEqualTo("declared");
            ProjectPackageTypeView recordType = (ProjectPackageTypeView) described.getDeclaration().orElseThrow();
            assertThat(recordType.getQualifiedName()).isEqualTo(name("p", "rec_t"));
            assertThat(recordType.getShape()).contains(ProjectTypeShape.RECORD);
            assertThat(recordType.getId()).isEqualTo(declarationId(snapshot, "p", "rec_t"));
            assertThat(recordType.getRecordFields()).extracting(f -> f.getName().getOriginalSpelling()).containsExactly("id", "label");
            assertThat(label(snapshot.describeType(recordType.getRecordFields().get(0).getTypeRef(), p).getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
            assertThat(label(snapshot.describeType(recordType.getRecordFields().get(1).getTypeRef(), p).getBuiltInCategory().orElseThrow())).isEqualTo("character");
        }
        // A record has no element; bounds and index domains are not part of the contract.
        assertThat(((ProjectShapedTypeView) snapshot.resolveNamedType(name("rec_t"), p).getDeclaration().orElseThrow()).getElementType()).isEmpty();
        assertThat(Arrays.stream(ProjectShapedTypeView.class.getMethods()).map(java.lang.reflect.Method::getName))
            .containsExactlyInAnyOrder("getShape", "getElementType", "getId", "getKind", "getRole", "getQualifiedName", "getFileId", "getSourceRange");

        // Standalone collections use the standalone namespace (null package context).
        ProjectShapedTypeView ids = (ProjectShapedTypeView) snapshot.resolveNamedType(name("ids")).getDeclaration().orElseThrow();
        assertThat(label(snapshot.describeType(ids.getElementType().orElseThrow(), null).getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
        ProjectShapedTypeView foreign = (ProjectShapedTypeView) snapshot.resolveNamedType(name("foreign_rows")).getDeclaration().orElseThrow();
        ProjectTypeDescription external = snapshot.describeType(foreign.getElementType().orElseThrow(), null);
        assertThat(label(external.getStatus())).isEqualTo("unresolved");
        assertThat(external.getResolution().orElseThrow().getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);
    }

    @Test
    void descriptionsStopConservatively() {
        ProjectSnapshot snapshot = types(counted("db/types_nested.sql", "types_nested.sql"));
        ProjectQualifiedName p = name("p");
        ProjectPackageTypeView stop = (ProjectPackageTypeView) snapshot.resolveNamedType(name("stop_t"), p).getDeclaration().orElseThrow();
        List<ProjectRecordFieldView> fields = stop.getRecordFields();
        ProjectTypeDescription external = snapshot.describeType(fields.get(0).getTypeRef(), p);
        assertThat(label(external.getStatus())).isEqualTo("unresolved");
        assertThat(external.getResolution().orElseThrow().getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);
        assertThat(external.getDeclaration()).isEmpty();
        // A source-backed %TYPE that names no supplied value is unresolved, not invalid.
        ProjectTypeDescription column = snapshot.describeType(fields.get(1).getTypeRef(), p);
        assertThat(label(column.getStatus())).isEqualTo("unresolved value");
        assertThat(column.getValueResolution().orElseThrow().getStatus()).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT);
        ProjectTypeDescription row = snapshot.describeType(fields.get(2).getTypeRef(), p);
        assertThat(row.getStatus()).isEqualTo(ProjectTypeDescription.Status.ANCHORED_REFERENCE);
        assertThat(row.getAnchorStop()).contains(ProjectTypeDescription.AnchorStop.ROWTYPE);
        assertThat(row.getTerminalReference().orElseThrow().getAnchor()).contains(ProjectTypeReference.Anchor.ROWTYPE);
        assertThat(snapshot.describeType(fields.get(3).getTypeRef(), p).getStatus()).isEqualTo(ProjectTypeDescription.Status.REF_REFERENCE);

        ProjectTypeDescription cycle = snapshot.describeType(name("c1"), p);
        assertThat(label(cycle.getStatus())).isEqualTo("cycle");
        assertThat(cycle.getSubtypes()).extracting(s -> s.getLocalName().getOriginalSpelling()).containsExactly("c1", "c2");
    }

    @Test
    void duplicatesAndFailedSourcesAreReportedThroughTheNamedResolution() {
        ProjectSnapshot duplicate = types(counted("db/a.sql", "types_nested.sql"), counted("db/b.sql", "types_nested.sql"));
        ProjectTypeDescription ambiguous = duplicate.describeType(name("amount"), name("p"));
        assertThat(ambiguous.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME);
        assertThat(ambiguous.getResolution().orElseThrow().getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.AMBIGUOUS);
        assertThat(ambiguous.getResolution().orElseThrow().getCandidates()).hasSize(2);
        assertThat(ambiguous.getSubtypes()).isEmpty();

        ProjectSnapshot failed = types(counted("db/types_nested.sql", "types_nested.sql"), failing("db/broken.sql"));
        ProjectTypeDescription incomplete = failed.describeType(name("amount"), name("p"));
        assertThat(incomplete.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME);
        assertThat(incomplete.getResolution().orElseThrow().getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.INCOMPLETE_INDEX);
        assertThat(incomplete.getResolution().orElseThrow().getFailures()).hasSize(1);
        // A built-in needs no complete project.
        ProjectTypeDescription builtIn = failed.describeType(name("NUMBER"), name("p"));
        assertThat(builtIn.getStatus()).isEqualTo(ProjectTypeDescription.Status.BUILT_IN);
        assertThat(builtIn.getBuiltInCategory()).contains(ProjectBuiltInTypeCategory.NUMERIC);
    }

    // ---- Source-backed %TYPE anchors and package-value lookup ----

    private static ProjectSnapshot anchors(ProjectSourceInput... more) {
        List<ProjectSourceInput> inputs = new ArrayList<>(List.of(counted("db/anchors_p.sql", "anchors_p.sql"),
            counted("db/anchors_q.sql", "anchors_q.sql"), counted("db/anchors_extra.sql", "anchors_extra.sql")));
        inputs.addAll(List.of(more));
        return ProjectSnapshots.prepare(inputs, false);
    }

    private static ProjectPackageValueView valueOf(ProjectSnapshot snapshot, String owner, String local) {
        ProjectValueResolution resolution = snapshot.resolvePackageValue(name(owner, local), null);
        assertThat(resolution.getStatus()).isEqualTo(ProjectValueResolution.Status.RESOLVED);
        return resolution.getValue().orElseThrow();
    }

    /** The retained type reference of a package value, described with the value's own owner. */
    private static ProjectTypeDescription describeValue(ProjectSnapshot snapshot, String owner, String local) {
        ProjectPackageValueView value = valueOf(snapshot, owner, local);
        return snapshot.describeType(value.getTypeRef(), value.getOwner());
    }

    // Deliberately exhaustive and without a default branch.
    private static String label(ProjectTypeDescription.AnchorStop stop) {
        return switch (stop) {
            case ROWTYPE -> "rowtype";
            case FIELD_PATH -> "field path";
            case ENCLOSING_SCOPE -> "enclosing scope";
            case DECLARED_LATER -> "declared later";
        };
    }

    private static String label(ProjectValueResolution.Status status) {
        return switch (status) {
            case RESOLVED -> "resolved";
            case AMBIGUOUS -> "ambiguous";
            case NOT_FOUND_IN_PROJECT -> "not found";
            case INCOMPLETE_INDEX -> "incomplete";
            case NOT_PREPARED -> "not prepared";
        };
    }

    @Test
    void packageValueLookupResolvesWithIdentityAndImmutableCandidates() {
        ProjectSnapshot snapshot = anchors();
        ProjectValueResolution g = snapshot.resolvePackageValue(name("p", "g"), null);
        assertThat(label(g.getStatus())).isEqualTo("resolved");
        ProjectPackageValueView value = g.getValue().orElseThrow();
        assertThat(value.getId()).isEqualTo(declarationId(snapshot, "p", "g"));
        assertThat(value.getTypeRef().getName()).isEqualTo(name("NUMBER"));
        assertThat(value.getSourceRange().getFileId()).isEqualTo("db/anchors_p.sql");
        assertThat(value.isConstant()).isFalse();
        assertThat(g.getCandidates()).extracting(ProjectDeclarationView::getId).containsExactly(value.getId());
        assertThat(g.getFailures()).isEmpty();
        ProjectValueResolution constant = snapshot.resolvePackageValue(name("c"), name("p"));
        assertThat(constant.getValue().orElseThrow().isConstant()).isTrue();
        assertThat(constant.getPackageOwner()).contains(name("p"));
        List<ProjectDeclarationView> candidates = g.getCandidates();
        assertThatThrownBy(candidates::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void anchoredValuesDescribeBuiltInTerminalsWithProvenance() {
        ProjectSnapshot snapshot = anchors();
        ProjectPackageValueView value = snapshot.resolvePackageValue(name("p", "g"), null).getValue().orElseThrow();
        ProjectTypeDescription a = describeValue(snapshot, "q", "a");
        assertThat(label(a.getStatus())).isEqualTo("built-in");
        assertThat(label(a.getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
        assertThat(a.getValues()).extracting(ProjectPackageValueView::getId).containsExactly(value.getId());
        assertThat(a.getSteps()).extracting(ProjectDeclarationView::getId).containsExactly(value.getId());
        assertThat(a.getTerminalReference().orElseThrow().getSourceRange()).isEqualTo(value.getTypeRef().getSourceRange());
        assertThat(a.getValueResolution()).isEmpty();
        assertThat(a.getAnchorStop()).isEmpty();
        assertThat(label(describeValue(snapshot, "q", "b").getBuiltInCategory().orElseThrow())).isEqualTo("character");
    }

    @Test
    void anchorChainsKeepOrderedProvenanceAndImmutableLists() {
        ProjectSnapshot snapshot = anchors();
        ProjectTypeDescription chain = describeValue(snapshot, "q", "t3");
        assertThat(label(chain.getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
        assertThat(chain.getValues()).extracting(v -> v.getLocalName().getOriginalSpelling())
            .containsExactly("third_value", "second_value", "first_value");
        assertThat(chain.getSubtypes()).extracting(s -> s.getLocalName().getOriginalSpelling()).containsExactly("numeric_t");
        assertThat(chain.getSteps()).extracting(ProjectDeclarationView::getId).containsExactly(
            declarationId(snapshot, "p", "third_value"), declarationId(snapshot, "p", "second_value"),
            declarationId(snapshot, "p", "first_value"), declarationId(snapshot, "p", "numeric_t"));
        assertThat(chain.getTerminalReference().orElseThrow().getName()).isEqualTo(name("NUMBER"));
        List<ProjectDeclarationView> steps = chain.getSteps();
        List<ProjectPackageValueView> values = chain.getValues();
        List<ProjectSubtypeView> subtypes = chain.getSubtypes();
        assertThatThrownBy(steps::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(values::clear).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(subtypes::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void recordAndCollectionTerminalsAreDescribedThroughTheirViews() {
        ProjectSnapshot snapshot = anchors();
        ProjectTypeDescription recordDescription = describeValue(snapshot, "q", "r");
        assertThat(label(recordDescription.getStatus())).isEqualTo("declared");
        ProjectPackageTypeView recType = (ProjectPackageTypeView) recordDescription.getDeclaration().orElseThrow();
        assertThat(recType.getId()).isEqualTo(declarationId(snapshot, "p", "rec_t"));
        assertThat(recType.getRecordFields()).extracting(f -> f.getName().getOriginalSpelling()).containsExactly("id");
        ProjectShapedTypeView rows = (ProjectShapedTypeView) describeValue(snapshot, "q", "rs").getDeclaration().orElseThrow();
        assertThat(rows.getShape()).contains(ProjectTypeShape.COLLECTION);
        assertThat(rows.getElementType().orElseThrow().getName()).isEqualTo(name("rec_t"));
    }

    @Test
    void nameBasedDescriptionKeepsSubtypeMeaningAndComposesWithValueLookup() {
        ProjectSnapshot snapshot = anchors();
        // The name-based overload keeps its S4 meaning (subtype aliases, no anchors).
        ProjectTypeDescription byName = snapshot.describeType(name("numeric_t"), name("p"));
        assertThat(byName.getStatus()).isEqualTo(ProjectTypeDescription.Status.BUILT_IN);
        assertThat(byName.getSubtypes()).hasSize(1);

        // A consumer that has already applied its own lexical scope composes the two public queries.
        ProjectPackageValueView resolved = snapshot.resolvePackageValue(name("p", "second_value"), null).getValue().orElseThrow();
        ProjectTypeDescription composed = snapshot.describeType(resolved.getTypeRef(), resolved.getOwner());
        assertThat(composed.getValues()).extracting(ProjectPackageValueView::getId).containsExactly(declarationId(snapshot, "p", "first_value"));
        assertThat(label(composed.getBuiltInCategory().orElseThrow())).isEqualTo("numeric");
    }

    @Test
    void ambiguousAndMissingValueLookupsReportConservativeStates() {
        ProjectSnapshot snapshot = anchors(counted("db/dd1.sql", "anchors_dup.sql"), counted("db/dd2.sql", "anchors_dup.sql"));
        ProjectValueResolution duplicate = snapshot.resolvePackageValue(name("dd", "g"), null);
        assertThat(label(duplicate.getStatus())).isEqualTo("ambiguous");
        assertThat(duplicate.getValue()).isEmpty();
        assertThat(duplicate.getCandidates()).hasSize(2);
        ProjectValueResolution collision = snapshot.resolvePackageValue(name("dup"), name("k"));
        assertThat(collision.getStatus()).isEqualTo(ProjectValueResolution.Status.AMBIGUOUS);
        assertThat(collision.getCandidates()).extracting(ProjectDeclarationView::getKind)
            .containsExactly(ProjectDeclarationKind.PACKAGE_VARIABLE, ProjectDeclarationKind.PACKAGE_PROCEDURE);
        ProjectValueResolution missing = snapshot.resolvePackageValue(name("p", "nothing"), null);
        assertThat(label(missing.getStatus())).isEqualTo("not found");
        assertThat(snapshot.resolvePackageValue(name("g"), null).getStatus()).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT);
    }

    @Test
    void unresolvedAndStoppedAnchorsExplainWhyTheyStop() {
        ProjectSnapshot snapshot = anchors(counted("db/dd1.sql", "anchors_dup.sql"), counted("db/dd2.sql", "anchors_dup.sql"));
        ProjectTypeDescription column = describeValue(snapshot, "q", "col");
        assertThat(column.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_VALUE);
        assertThat(column.getValueResolution().orElseThrow().getStatus()).isEqualTo(ProjectValueResolution.Status.NOT_FOUND_IN_PROJECT);
        assertThat(column.getAnchorStop()).isEmpty();
        assertThat(column.getTerminalReference().orElseThrow().getName()).isEqualTo(name("employees", "salary"));

        ProjectTypeDescription row = describeValue(snapshot, "p", "row_ref");
        assertThat(row.getStatus()).isEqualTo(ProjectTypeDescription.Status.ANCHORED_REFERENCE);
        assertThat(label(row.getAnchorStop().orElseThrow())).isEqualTo("rowtype");
        assertThat(row.getValueResolution()).isEmpty();
        assertThat(label(describeValue(snapshot, "p", "field_ref").getAnchorStop().orElseThrow())).isEqualTo("field path");
        assertThat(label(describeValue(snapshot, "p", "later_ref").getAnchorStop().orElseThrow())).isEqualTo("declared later");
        ProjectPackageTypeView cap = (ProjectPackageTypeView) snapshot.resolveNamedType(name("cap_t"), name("p")).getDeclaration().orElseThrow();
        ProjectTypeDescription captured = snapshot.describeType(cap.getRecordFields().get(1).getTypeRef(), cap.getOwner());
        assertThat(label(captured.getAnchorStop().orElseThrow())).isEqualTo("enclosing scope");
    }

    @Test
    void cyclesAndUnknownNamesKeepTheValuesFollowedSoFar() {
        ProjectSnapshot snapshot = anchors(counted("db/dd1.sql", "anchors_dup.sql"), counted("db/dd2.sql", "anchors_dup.sql"));
        ProjectTypeDescription cycle = describeValue(snapshot, "cy1", "x");
        assertThat(cycle.getStatus()).isEqualTo(ProjectTypeDescription.Status.CYCLE);
        assertThat(cycle.getValues()).hasSize(2);
        ProjectTypeDescription unknown = describeValue(snapshot, "p", "unknown_ref");
        assertThat(unknown.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_NAME);
        assertThat(unknown.getValues()).extracting(v -> v.getLocalName().getOriginalSpelling()).containsExactly("unknown_value");
        assertThat(unknown.getResolution().orElseThrow().getStatus()).isEqualTo(ProjectNamedTypeResolution.Status.NOT_FOUND_IN_PROJECT);
    }

    @Test
    void failedSourcesMakeValueLookupAndAnchorsIncomplete() {
        ProjectSnapshot failed = anchors(failing("db/broken.sql"));
        ProjectValueResolution incomplete = failed.resolvePackageValue(name("p", "g"), null);
        assertThat(label(incomplete.getStatus())).isEqualTo("incomplete");
        assertThat(incomplete.getValue()).isEmpty();
        assertThat(incomplete.getFailures()).hasSize(1);
        assertThat(incomplete.getCandidates()).hasSize(1);
        // With a failed source the consumer's own value is still in the inventory, but the target lookup is incomplete.
        ProjectPackageValueView own = (ProjectPackageValueView) failed.findDeclarations(name("q", "a")).getDeclarations().get(0);
        ProjectTypeDescription unresolved = failed.describeType(own.getTypeRef(), own.getOwner());
        assertThat(unresolved.getStatus()).isEqualTo(ProjectTypeDescription.Status.UNRESOLVED_VALUE);
        assertThat(unresolved.getValueResolution().orElseThrow().getStatus()).isEqualTo(ProjectValueResolution.Status.INCOMPLETE_INDEX);
        assertThat(failed.describeType(name("NUMBER"), null).getStatus()).isEqualTo(ProjectTypeDescription.Status.BUILT_IN);
    }

    @Test
    void theAnchorApiHasTheDocumentedJavaSignatures() throws Exception {
        Class<ProjectTypeDescription> type = ProjectTypeDescription.class;
        assertThat(type.getMethod("getSteps").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectDeclarationView>");
        assertThat(type.getMethod("getValues").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectPackageValueView>");
        assertThat(type.getMethod("getSubtypes").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectSubtypeView>");
        assertThat(type.getMethod("getValueResolution").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectValueResolution>");
        assertThat(type.getMethod("getAnchorStop").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectTypeDescription$AnchorStop>");
        assertThat(ProjectSnapshot.class.getMethod("resolvePackageValue", ProjectQualifiedName.class, ProjectQualifiedName.class).getReturnType())
            .isEqualTo(ProjectValueResolution.class);
        assertThat(ProjectValueResolution.class.getMethod("getValue").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.Optional<com.felipebz.zpa.api.project.ProjectPackageValueView>");
        assertThat(ProjectValueResolution.class.getMethod("getCandidates").getGenericReturnType().getTypeName())
            .isEqualTo("java.util.List<com.felipebz.zpa.api.project.ProjectDeclarationView>");
        assertThat(Arrays.stream(ProjectTypeDescription.Status.values()).map(Enum::name)).containsExactly(
            "DECLARED_TYPE", "BUILT_IN", "UNRESOLVED_NAME", "ANCHORED_REFERENCE", "REF_REFERENCE", "CYCLE", "UNRESOLVED_VALUE");
        assertThat(Arrays.stream(ProjectTypeDescription.AnchorStop.values()).map(Enum::name))
            .containsExactly("ROWTYPE", "FIELD_PATH", "ENCLOSING_SCOPE", "DECLARED_LATER");
        assertThat(Arrays.stream(ProjectValueResolution.Status.values()).map(Enum::name)).containsExactly(
            "RESOLVED", "AMBIGUOUS", "NOT_FOUND_IN_PROJECT", "INCOMPLETE_INDEX", "NOT_PREPARED");
    }
}
