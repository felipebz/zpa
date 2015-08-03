package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.sslr.ast.AstSelect;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

@Rule(
    key = SelectWithRownumAndOrderByCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("20min")
@ActivatedByDefault
public class SelectWithRownumAndOrderByCheck extends BaseCheck {

    public static final String CHECK_KEY = "SelectWithRownumAndOrderBy";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        if (!node.hasDirectChildren(PlSqlGrammar.ORDER_BY_CLAUSE)) return;
        
        AstSelect whereClause = node.select().children(PlSqlGrammar.WHERE_CLAUSE);
        if (whereClause.isEmpty()) return;
        
        AstSelect whereComparisonConditions = node.select().descendants(PlSqlGrammar.COMPARISON_EXPRESSION);
        if (whereComparisonConditions.isEmpty()) return;
        
        for (AstNode comparison : whereComparisonConditions) {
            AstSelect children = comparison.select().children(PlSqlGrammar.IDENTIFIER_NAME);
            for (AstNode child : children) {
                if ("rownum".equalsIgnoreCase(child.getTokenValue())) {
                    getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), child);
                }
            }
        }
    }

}
