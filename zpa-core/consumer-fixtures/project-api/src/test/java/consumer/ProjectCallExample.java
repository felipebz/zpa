package consumer;

import com.felipebz.zpa.api.project.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Documentation example (Java): find call candidates, resolve a call and inspect the evidence. */
final class ProjectCallExample {
    private ProjectCallExample() {
    }

    /** Every public procedure/function named billing.charge; nothing is selected. */
    static List<String> listCandidates(ProjectSnapshot snapshot) {
        ProjectCallTarget target = ProjectCallTarget.packageMember(
            ProjectQualifiedName.of("billing"), ProjectIdentifier.fromSource("charge"));
        ProjectCallCandidates found = snapshot.findCallCandidates(target);
        List<String> report = new ArrayList<>();
        report.add("status " + found.getStatus());
        for (ProjectSubprogramView candidate : found.getCandidates()) {
            report.add(candidate.getRole() + " " + signature(candidate));
        }
        return report;
    }

    /** billing.charge(1, amount => 25): positional then named. */
    static ProjectCallArguments mixedArguments() {
        return ProjectCallArguments.of(List.of(
            ProjectCallArgument.positional(),
            ProjectCallArgument.named(ProjectIdentifier.fromSource("amount"))));
    }

    /** Explains any resolution; it never picks a candidate the API did not select. */
    static List<String> explain(ProjectSnapshot snapshot, ProjectCallTarget target, ProjectCallArguments arguments) {
        ProjectCallResolution resolution = snapshot.resolveCall(target, ProjectCallKind.PROCEDURE, arguments);
        List<String> report = new ArrayList<>();
        switch (resolution.getStatus()) {
            case RESOLVED -> report.add("calls " + signature(resolution.getSelected().orElseThrow().getDeclaration()));
            case AMBIGUOUS -> report.add("several overloads accept these arguments");
            case NO_APPLICABLE_CANDIDATE, WRONG_KIND -> report.add("no candidate accepts this call");
            case INVALID_NOTATION -> report.add("invalid notation at argument "
                + arguments.getInvalidArgumentOrdinal().getAsInt());
            case AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS -> report.add("target not settled: " + resolution.getUncertainties());
            case INCOMPLETE_INDEX -> report.add("unknown: sources failed " + resolution.getFailures().stream()
                .map(ProjectPreparationFailure::getFileId).toList());
            case UNKNOWN_PACKAGE, NOT_FOUND_IN_PROJECT -> report.add("not in the supplied sources");
            case NOT_PREPARED -> report.add("no project was prepared");
        }
        for (ProjectCallCandidate candidate : resolution.getCandidates()) {
            StringBuilder line = new StringBuilder(signature(candidate.getDeclaration()) + ": " + candidate.getApplicability());
            for (ProjectArgumentBinding binding : candidate.getBindings()) {
                line.append(" #").append(binding.getArgumentOrdinal()).append("->").append(binding.getParameter().getName());
            }
            candidate.getOmittedParameters().forEach(p -> line.append(" default ").append(p.getName()));
            candidate.getMissingParameters().forEach(p -> line.append(" missing ").append(p.getName()));
            candidate.getRejectedArgumentOrdinal().ifPresent(o -> line.append(" rejects #").append(o));
            report.add(line.toString());
        }
        return report;
    }

    /** The walkthrough printed on the documentation page, verbatim. */
    static void documentedWalkthrough(String spec, String body, ProjectCallArguments arguments) {
        ProjectSnapshot snapshot = ProjectSnapshots.prepare(List.of(
            ProjectSourceInput.ofText("billing.pks", spec), ProjectSourceInput.ofText("billing.pkb", body)));
        ProjectCallTarget charge = ProjectCallTarget.packageMember(
            ProjectQualifiedName.of("billing"), ProjectIdentifier.fromSource("charge"));

        ProjectCallCandidates found = snapshot.findCallCandidates(charge);
        for (ProjectSubprogramView candidate : found.getCandidates()) {
            System.out.println(candidate.getRole() + " " + candidate.getLocalName());   // specifications only
        }

        ProjectCallResolution resolution = snapshot.resolveCall(charge, ProjectCallKind.PROCEDURE, arguments);
        switch (resolution.getStatus()) {
            case RESOLVED -> System.out.println("calls " + resolution.getSelected().orElseThrow().getDeclaration().getId());
            case AMBIGUOUS -> System.out.println("several overloads accept these arguments");
            case NO_APPLICABLE_CANDIDATE, WRONG_KIND -> System.out.println("no candidate accepts this call");
            case INVALID_NOTATION -> System.out.println("invalid notation at argument "
                + arguments.getInvalidArgumentOrdinal().getAsInt());
            case AMBIGUOUS_TARGET, PARTIAL_DECLARATIONS -> System.out.println("target not settled: " + resolution.getUncertainties());
            case INCOMPLETE_INDEX -> System.out.println("unknown: " + resolution.getFailures().size() + " source(s) failed");
            case UNKNOWN_PACKAGE, NOT_FOUND_IN_PROJECT -> System.out.println("not in the supplied sources");
            case NOT_PREPARED -> System.out.println("no project was prepared");
        }
        for (ProjectCallCandidate candidate : resolution.getCandidates()) {
            System.out.println(candidate.getDeclaration().getId() + ": " + candidate.getApplicability());
            candidate.getBindings().forEach(b ->
                System.out.println("  #" + b.getArgumentOrdinal() + " -> " + b.getParameter().getName()));
            candidate.getOmittedParameters().forEach(p -> System.out.println("  default " + p.getName()));
            candidate.getMissingParameters().forEach(p -> System.out.println("  missing " + p.getName()));
            candidate.getRejectedArgumentOrdinal().ifPresent(o -> System.out.println("  rejects #" + o));
        }
    }

    private static String signature(ProjectSubprogramView callable) {
        return callable.getLocalName() + callable.getParameters().stream()
            .map(ProjectParameterView::getName).map(Object::toString).collect(Collectors.joining(", ", "(", ")"));
    }
}
