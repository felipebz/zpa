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
    key = VariableInitializationWithNullCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class VariableInitializationWithNullCheck extends BaseCheck {
    public static final String CHECK_KEY = "VariableInitializationWithNull";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.DEFAULT_VALUE_ASSIGNMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (node.hasParent(PlSqlGrammar.VARIABLE_DECLARATION, PlSqlGrammar.RECORD_FIELD_DECLARATION)) {
            AstNode expression = node.getLastChild();
            if (CheckUtils.isNullLiteralOrEmptyString(expression)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        }
    }

}
