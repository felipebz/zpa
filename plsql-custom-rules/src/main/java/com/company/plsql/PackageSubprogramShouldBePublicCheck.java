package com.company.plsql;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.annotations.ActivatedByDefault;
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation;
import org.sonar.plugins.plsqlopen.api.annotations.Priority;
import org.sonar.plugins.plsqlopen.api.annotations.Rule;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.project.PackageSpecificationResolution;
import org.sonar.plugins.plsqlopen.api.sslr.AstNode;

@Rule(
    name = "Package body subprograms should be public",
    description = "Package body subprograms should also be declared in the package specification.",
    key = "PackageSubprogramShouldBePublicCheck",
    priority = Priority.MAJOR
)
@ConstantRemediation("10min")
@ActivatedByDefault
public class PackageSubprogramShouldBePublicCheck extends PlSqlCheck {

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.PROCEDURE_DECLARATION, PlSqlGrammar.FUNCTION_DECLARATION);
    }

    @Override
    public void visitNode(AstNode node) {
        PackageSpecificationResolution resolution = projectAnalysis().resolvePackageSpecification(node);
        if (resolution.getStatus() == PackageSpecificationResolution.Status.NOT_FOUND) {
            addIssue(node, "Declare this package subprogram in the package specification.");
        }
    }
}
