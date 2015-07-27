package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

@Rule(
    key = ComparisonWithNullCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    name = "Do not compare values against the NULL literal or an empty string.",
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class ComparisonWithNullCheck extends SquidCheck<Grammar> {
    public static final String CHECK_KEY = "ComparisonWithNull";
    private static final String MESSAGE = "Change this comparison to \"IS NULL\" or \"IS NOT NULL\".";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> children = node.getChildren(PlSqlGrammar.LITERAL);
        for (AstNode child : children) {
            if (child.hasDirectChildren(PlSqlGrammar.NULL_LITERAL)) {
                registerViolation(child);
                continue;
            }
            
            AstNode characterLiteral = child.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL);
            if (characterLiteral != null && characterLiteral.getTokenValue().equals("''")) {
                registerViolation(child);
            }
        }
    }
    
    private void registerViolation(AstNode node) {
        getContext().createLineViolation(this, MESSAGE, node);
    }
}
