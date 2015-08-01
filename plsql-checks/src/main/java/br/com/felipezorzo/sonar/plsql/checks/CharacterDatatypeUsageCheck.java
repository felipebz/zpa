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
    key = CharacterDatatypeUsageCheck.CHECK_KEY,
    priority = Priority.MINOR,
    tags = Tags.OBSOLETE
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_CHANGEABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class CharacterDatatypeUsageCheck extends BaseCheck {

    public static final String CHECK_KEY = "CharacterDatatypeUsage";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.CHARACTER_DATAYPE);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode datatype = node.getFirstChild();
        if (datatype.is(PlSqlKeyword.CHAR, PlSqlKeyword.VARCHAR)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node, datatype.getTokenValue());
        }
    }

}
