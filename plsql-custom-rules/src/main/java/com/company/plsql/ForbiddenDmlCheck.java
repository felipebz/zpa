package com.company.plsql;

import org.sonar.plugins.plsqlopen.api.annotations.Priority;
import org.sonar.plugins.plsqlopen.api.annotations.Rule;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.annotations.ActivatedByDefault;
import org.sonar.plugins.plsqlopen.api.annotations.ConstantRemediation;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.sslr.AstNode;

@Rule(
    name = "Avoid DML on table USER",
    description = "You should use the functions from the USER_WRAPPER package.",
    key = "ForbiddenDmlCheck",
    priority = Priority.MAJOR
)
@ConstantRemediation("10min")
@ActivatedByDefault
public class ForbiddenDmlCheck extends PlSqlCheck {
    
    @Override
    public void init() {
        subscribeTo(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode table = node.getFirstChildOrNull(DmlGrammar.TABLE_REFERENCE);
        
        if (table != null && table.getTokenOriginalValue().equalsIgnoreCase("user")) {
            addIssue(table, "Replace this query by a function of the USER_WRAPPER package.");
        }
    }

}
