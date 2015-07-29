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
    key = InsertWithoutColumnsCheck.CHECK_KEY,
    priority = Priority.CRITICAL,
    tags = Tags.CONVENTION
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class InsertWithoutColumnsCheck extends BaseCheck  {
    public static final String CHECK_KEY = "InsertWithoutColumns";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.INSERT_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        if (!node.hasDirectChildren(PlSqlGrammar.INSERT_COLUMNS)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
        }
    }
}
