package com.felipebz.zpa.probe;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.annotations.Priority;
import org.sonar.plugins.plsqlopen.api.annotations.Rule;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.project.*;
import org.sonar.plugins.plsqlopen.api.sslr.AstNode;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Test-only semantic check, written as a third-party rule plugin would be: for each package value declared by the
 * visited package, it describes the {@code %TYPE}/{@code %ROWTYPE} with the run's external context and reports the
 * description as an issue message, e.g. {@code hr_api.v_salary RESOLVED NUMBER(10,2)}.
 */
@Rule(key = "ExternalTypeProbe", name = "External type probe", description = "Test only", priority = Priority.INFO)
public class ExternalTypeProbeCheck extends PlSqlCheck {

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.CREATE_PACKAGE);
    }

    @Override
    public void visitNode(AstNode node) {
        ProjectAnalysis analysis = projectAnalysis();
        ProjectExternalContext external = analysis.getExternalContext();
        ProjectSnapshot snapshot = analysis.getSnapshot();
        String packageName = node.getFirstChild(PlSqlGrammar.UNIT_NAME).getDescendants(PlSqlGrammar.IDENTIFIER_NAME).stream()
            .map(AstNode::getTokenOriginalValue).collect(Collectors.joining(".")).toLowerCase(Locale.ROOT);
        for (ProjectDeclarationView view : snapshot.getDeclarations()) {
            if (!(view instanceof ProjectPackageValueView value)
                || !value.getOwner().toString().toLowerCase(Locale.ROOT).equals(packageName)) {
                continue;
            }
            ProjectExternalTypeDescription description = snapshot.describeExternalType(
                value.getTypeRef(), value.getOwner(), external.getMetadata(), external.compilationSchema(value.getOwner()).orElse(null));
            addIssue(node, value.getQualifiedName() + " " + render(description));
        }
    }

    private static String render(ProjectExternalTypeDescription description) {
        if (description.getStatus() != ProjectExternalTypeDescription.Status.RESOLVED) {
            return description.getStatus().name();
        }
        ProjectExternalLookup lookup = description.getLookup().get();
        if (description.getAnchor().orElse(null) == ProjectTypeReference.Anchor.ROWTYPE) {
            return "RESOLVED " + lookup.getRelation().get().getRowFields().stream()
                .map(f -> f.getName().getOriginalSpelling().replace("\"", "") + " " + f.getDatatype().map(ExternalTypeProbeCheck::render).orElse("?"))
                .collect(Collectors.joining(", ", "(", ")"));
        }
        return "RESOLVED " + lookup.getColumn().get().getDatatype().map(ExternalTypeProbeCheck::render).orElse("?");
    }

    private static String render(ProjectExternalDatatype type) {
        if (type.getLength().isPresent()) {
            return type.getName() + "(" + type.getLength().getAsInt() + type.getLengthSemantics().map(s -> " " + s).orElse("") + ")";
        }
        if (type.getPrecision().isPresent() && type.getScale().isPresent()) {
            return type.getName() + "(" + type.getPrecision().getAsInt() + "," + type.getScale().getAsInt() + ")";
        }
        if (type.getPrecision().isPresent()) {
            return type.getName() + "(" + type.getPrecision().getAsInt() + ")";
        }
        return type.getName();
    }
}
