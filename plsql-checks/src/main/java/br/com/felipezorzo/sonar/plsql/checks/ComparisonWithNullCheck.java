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
import br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator;

@Rule(
    key = ComparisonWithNullCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class ComparisonWithNullCheck extends BaseCheck {
    public static final String CHECK_KEY = "ComparisonWithNull";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> children = node.getChildren(PlSqlGrammar.LITERAL);
        for (AstNode child : children) {
            if (CheckUtils.isNullLiteralOrEmptyString(child)) {
                String suggestion = null;
                AstNode operator = node.getFirstChild(PlSqlGrammar.RELATIONAL_OPERATOR);
                if (operator.getFirstChild().is(PlSqlPunctuator.EQUALS)) {
                    suggestion = "IS NULL";
                } else {
                    suggestion = "IS NOT NULL";  
                }
                
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, suggestion);
                continue;
            }
        }
    }
}
