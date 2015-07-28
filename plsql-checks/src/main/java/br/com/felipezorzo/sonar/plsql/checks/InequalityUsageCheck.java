package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator;

@Rule(
    key = InequalityUsageCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.OBSOLETE
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LANGUAGE_RELATED_PORTABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class InequalityUsageCheck extends BaseCheck {
    public static final String CHECK_KEY = "InequalityUsage";

    @Override
    public void init() {
        subscribeTo(PlSqlPunctuator.NOTEQUALS2);
        subscribeTo(PlSqlPunctuator.NOTEQUALS3);
        subscribeTo(PlSqlPunctuator.NOTEQUALS4);
    }

    @Override
    public void visitNode(AstNode node) {
        getContext().createLineViolation(this, String.format(getLocalizedMessage(CHECK_KEY), node.getTokenValue()), node);
    }

}
