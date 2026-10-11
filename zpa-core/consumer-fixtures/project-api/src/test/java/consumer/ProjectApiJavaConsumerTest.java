package consumer;

import com.felipebz.zpa.api.project.ProjectDeclarationId;
import com.felipebz.zpa.api.project.ProjectDeclarationKind;
import com.felipebz.zpa.api.project.ProjectDeclarationRole;
import com.felipebz.zpa.api.project.ProjectDeclarationView;
import com.felipebz.zpa.api.project.ProjectFunctionView;
import com.felipebz.zpa.api.project.ProjectIdentifier;
import com.felipebz.zpa.api.project.ProjectInventory;
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution;
import com.felipebz.zpa.api.project.ProjectPackageSubprogramView;
import com.felipebz.zpa.api.project.ProjectPackageTypeView;
import com.felipebz.zpa.api.project.ProjectParameterMode;
import com.felipebz.zpa.api.project.ProjectParameterView;
import com.felipebz.zpa.api.project.ProjectPreparationState;
import com.felipebz.zpa.api.project.ProjectQualifiedName;
import com.felipebz.zpa.api.project.ProjectRecordFieldView;
import com.felipebz.zpa.api.project.ProjectSnapshot;
import com.felipebz.zpa.api.project.ProjectSnapshots;
import com.felipebz.zpa.api.project.ProjectSourceInput;
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation;
import com.felipebz.zpa.api.project.ProjectSubtypeView;
import com.felipebz.zpa.api.project.ProjectTypeReference;
import com.felipebz.zpa.api.project.ProjectTypeShape;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

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

        Map<ProjectDeclarationKind, Integer> kinds = new HashMap<>();
        for (ProjectDeclarationView view : snapshot.getDeclarations()) {
            kinds.merge(view.getKind(), 1, Integer::sum);
        }
        assertThat(kinds.get(ProjectDeclarationKind.PACKAGE)).isEqualTo(1);
        assertThat(kinds.get(ProjectDeclarationKind.PACKAGE_TYPE)).isEqualTo(1);
        assertThat(kinds.get(ProjectDeclarationKind.PACKAGE_SUBTYPE)).isEqualTo(1);
        assertThat(kinds.get(ProjectDeclarationKind.PACKAGE_PROCEDURE)).isEqualTo(5);
        assertThat(kinds.get(ProjectDeclarationKind.PACKAGE_FUNCTION)).isEqualTo(2);
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
        assertThat(place.getDeclarations()).hasSize(3); // two specifications and one body
        assertThat(place.getDeclarations()).extracting(ProjectPackageSubprogramView::getRole)
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
}
