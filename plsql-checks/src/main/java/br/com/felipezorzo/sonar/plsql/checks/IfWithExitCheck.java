package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;

@Rule(
    key = IfWithExitCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class IfWithExitCheck extends BaseCheck {
    
    public static final String CHECK_KEY = "IfWithExit";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.EXIT_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode statement = node.getParent();
        AstNode ifStatement = statement.getParent();
        if (ifStatement.is(PlSqlGrammar.IF_STATEMENT) &&
            !ifStatement.hasDirectChildren(PlSqlKeyword.ELSIF, PlSqlKeyword.ELSE) &&
            ifStatement.getChildren(PlSqlGrammar.STATEMENT).size() == 1) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
        }
    }

}
