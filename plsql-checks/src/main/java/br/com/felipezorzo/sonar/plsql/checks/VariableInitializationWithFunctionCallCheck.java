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
    key = VariableInitializationWithFunctionCallCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class VariableInitializationWithFunctionCallCheck extends BaseCheck {
    public static final String CHECK_KEY = "VariableInitializationWithFunctionCall";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (node.hasParent(PlSqlGrammar.VARIABLE_DECLARATION)) {
            AstNode expression = node.getLastChild();
            if (expression.is(PlSqlGrammar.METHOD_CALL)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        }
    }

}
