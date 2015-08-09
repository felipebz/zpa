package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

@Rule(
    key = EmptyStringAssignmentCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class EmptyStringAssignmentCheck extends BaseCheck {
    
    public static final String CHECK_KEY = "EmptyStringAssignment";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.ASSIGNMENT_STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        AstNode value = node.getFirstChild(PlSqlGrammar.LITERAL);
        if (value != null && CheckUtils.isEmptyString(value)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), value);
        }
    }

}
