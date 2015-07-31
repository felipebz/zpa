package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

@Rule(
    key = ComparisonWithBooleanCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault   
public class ComparisonWithBooleanCheck extends BaseCheck {
    public static final String CHECK_KEY = "ComparisonWithBoolean";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> children = node.getChildren(PlSqlGrammar.LITERAL);
        for (AstNode child : children) {
            if (child.getFirstChild().is(PlSqlGrammar.BOOLEAN_LITERAL)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, child.getTokenValue());
                return;
            }
        }
    }

}
