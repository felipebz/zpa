package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;

@Rule(
    key = IdenticalExpressionCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class IdenticalExpressionCheck extends BaseCheck {
    public static final String CHECK_KEY = "IdenticalExpression";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.COMPARISON_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
    	AstNode operator = node.getFirstChild(PlSqlGrammar.RELATIONAL_OPERATOR);
    	if (operator == null) return;
    	
    	AstNode leftSide = node.getFirstChild();
    	AstNode rightSide = node.getLastChild();
    	
    	if (CheckUtils.equalNodes(leftSide, rightSide)) {
    		getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, operator.getTokenValue());
    	}
    }

}
