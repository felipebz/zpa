package consumer;

import com.felipebz.zpa.api.project.ProjectDeclarationRole;
import com.felipebz.zpa.api.project.ProjectFunctionView;
import com.felipebz.zpa.api.project.ProjectIdentifier;
import com.felipebz.zpa.api.project.ProjectInventory;
import com.felipebz.zpa.api.project.ProjectNamedTypeResolution;
import com.felipebz.zpa.api.project.ProjectPackageSubprogramView;
import com.felipebz.zpa.api.project.ProjectPackageTypeView;
import com.felipebz.zpa.api.project.ProjectQualifiedName;
import com.felipebz.zpa.api.project.ProjectSnapshot;
import com.felipebz.zpa.api.project.ProjectSnapshots;
import com.felipebz.zpa.api.project.ProjectSourceInput;
import com.felipebz.zpa.api.project.ProjectSpecificationCorrelation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Documentation example (Java): prepare, resolve a type, inspect fields and callables, correlate a body. */
final class ProjectApiExample {
    static List<String> describeShop(List<ProjectSourceInput> sources) {
        List<String> report = new ArrayList<>();
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(sources);
        ProjectQualifiedName pkg = ProjectQualifiedName.of("shop");

        // Named type resolution: every status is handled; nothing picks "the first" candidate.
        ProjectNamedTypeResolution type = snapshot.resolveNamedType(ProjectQualifiedName.of("order_t"), pkg);
        switch (type.getStatus()) {
            case RESOLVED -> {
                if (type.getDeclaration().orElseThrow() instanceof ProjectPackageTypeView declaration) {
                    declaration.getRecordFields().forEach(f -> report.add(
                        "field " + f.getOrdinal() + ": " + f.getName().getOriginalSpelling() + " " + f.getTypeRef().getName()));
                }
            }
            case AMBIGUOUS -> report.add("order_t is declared " + type.getCandidates().size() + " times in the supplied sources");
            case INCOMPLETE_INDEX -> report.add("unknown: " + type.getFailures().size() + " source(s) could not be read");
            case NOT_FOUND_IN_PROJECT -> report.add("order_t is not in the supplied sources (not proof it is missing in Oracle)");
            case NOT_PREPARED -> report.add("no project was prepared");
        }

        // Unranked candidates: specifications and bodies are all returned.
        ProjectInventory<ProjectPackageSubprogramView> candidates =
            snapshot.findPackageSubprograms(pkg, ProjectIdentifier.fromSource("total"));
        for (ProjectPackageSubprogramView candidate : candidates.getDeclarations()) {
            String returns = candidate instanceof ProjectFunctionView function
                ? function.getReturnType().getName().toString() : "-";
            String parameters = candidate.getParameters().stream()
                .map(p -> p.getName().getOriginalSpelling()).collect(Collectors.joining(", "));
            report.add(candidate.getRole() + " total(" + parameters + ") returns " + returns);
        }

        // Correlation takes a BODY occurrence ID from the same snapshot.
        for (ProjectPackageSubprogramView body : candidates.getDeclarations()) {
            if (body.getRole() != ProjectDeclarationRole.BODY) {
                continue;
            }
            ProjectSpecificationCorrelation correlation = snapshot.correlateSpecification(body.getId());
            switch (correlation.getStatus()) {
                case RESOLVED -> report.add("body matches "
                    + correlation.getSpecification().orElseThrow().getSourceRange().getFileId());
                case NOT_FOUND -> report.add("no matching specification was supplied");
                case AMBIGUOUS -> report.add(correlation.getCandidates().size() + " matching specifications");
                case INCOMPLETE -> report.add("unknown: some sources failed");
            }
        }
        return report;
    }

    private ProjectApiExample() {
    }
}
