package com.company.plsql;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import org.sonar.plsqlopen.checks.AbstractBaseCheck;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;

import com.sonar.sslr.api.AstNode;

@Rule(
    name = "Avoid DML on table USER",
    description = "You should use the functions from the USER_WRAPPER package.",
    key = "ForbiddenDmlCheck",
    priority = Priority.MAJOR
)
@ConstantRemediation("10min")
@ActivatedByDefault
public class ForbiddenDmlCheck extends AbstractBaseCheck {
    
    @Override
    public void init() {
        subscribeTo(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode table = node.getFirstChild(DmlGrammar.TABLE_REFERENCE);
        
        if (table != null && table.getTokenOriginalValue().equalsIgnoreCase("user")) {
            getContext().createViolation(this, "Replace this query by a function of the USER_WRAPPER package.", table);
        }
    }

}
