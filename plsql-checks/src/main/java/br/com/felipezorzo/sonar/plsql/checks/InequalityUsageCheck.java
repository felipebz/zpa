package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;

import br.com.felipezorzo.sonar.plsql.api.PlSqlPunctuator;

@Rule(
    key = InequalityUsageCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    name = "Only \"<>\" should be used to test inequality.",
    tags = Tags.OBSOLETE
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LANGUAGE_RELATED_PORTABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class InequalityUsageCheck extends SquidCheck<Grammar> {
    public static final String CHECK_KEY = "InequalityUsage";
    private static final String MESSAGE = "Replace \"%s\" by \"<>\".";

    @Override
    public void init() {
        subscribeTo(PlSqlPunctuator.NOTEQUALS2);
        subscribeTo(PlSqlPunctuator.NOTEQUALS3);
        subscribeTo(PlSqlPunctuator.NOTEQUALS4);
    }

    @Override
    public void visitNode(AstNode node) {
        getContext().createLineViolation(this, String.format(MESSAGE, node.getTokenValue()), node);
    }

}
