package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator;

@Rule(
    key = DuplicatedValueInInCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class DuplicatedValueInInCheck extends BaseCheck {
    public static final String CHECK_KEY = "DuplicatedValueInIn";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IN_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        List<AstNode> values = getInValue(node);
        findSameValues(values);
    }
    
    public List<AstNode> getInValue(AstNode inExpression) {
        List<AstNode> values = Lists.newArrayList();
        AstNode current = inExpression.getFirstChild(PlSqlPunctuator.LPARENTHESIS);
        while (current != null) {
            current = current.getNextSibling();
            
            if (current.is(PlSqlPunctuator.RPARENTHESIS)) {
                current = null;
            } else if (!current.is(PlSqlPunctuator.COMMA)) {
                values.add(current);
            }
        }
        return values;
    }
    
    private void findSameValues(List<AstNode> values) {
        for (int i = 1; i < values.size(); i++) {
            checkValue(values, i);
        }
    }

    private void checkValue(List<AstNode> values, int index) {
        for (int j = 0; j < index; j++) {
            if (CheckUtils.equalNodes(values.get(j), values.get(index))) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), values.get(j));
                return;
            }
        }
    }

}
