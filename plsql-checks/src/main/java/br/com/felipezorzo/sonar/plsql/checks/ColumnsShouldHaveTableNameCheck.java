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
    key = ColumnsShouldHaveTableNameCheck.CHECK_KEY,
    priority = Priority.MAJOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class ColumnsShouldHaveTableNameCheck extends BaseCheck {

    public static final String CHECK_KEY = "ColumnsShouldHaveTableName";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_COLUMN);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode candidate = node.getFirstChild();
        
        AstNode selectExpression = node.getParent();
        if (selectExpression.getChildren(PlSqlGrammar.FROM_CLAUSE).size() == 1) return;
        
        if (candidate.is(PlSqlGrammar.IDENTIFIER_NAME)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), candidate);
        }
    }

}
