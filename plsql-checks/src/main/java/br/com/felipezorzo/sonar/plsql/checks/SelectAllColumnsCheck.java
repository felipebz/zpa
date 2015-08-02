package br.com.felipezorzo.sonar.plsql.checks;

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
    key = SelectAllColumnsCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.PERFORMANCE
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("30min")
@ActivatedByDefault
public class SelectAllColumnsCheck extends BaseCheck {

    public static final String CHECK_KEY = "SelectAllColumns";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_COLUMN);
    }

    @Override
    public void visitNode(AstNode node) {
        if (node.getParent().getParent().is(PlSqlGrammar.EXISTS_EXPRESSION)) return;
        
        AstNode candidate = node.getFirstChild();
        
        if (candidate.is(PlSqlGrammar.OBJECT_REFERENCE)) {
            candidate = candidate.getLastChild();
        }
        
        if (candidate.is(PlSqlPunctuator.MULTIPLICATION)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), candidate);
        }
    }
    
}
