package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.checks.common.BaseMethodCallChecker;

@Rule(
    key = ToDateWithoutFormatCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.TIME_ZONE_RELATED_PORTABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class ToDateWithoutFormatCheck extends BaseMethodCallChecker {
    public static final String CHECK_KEY = "ToDateWithoutFormat";

    @Override
    protected boolean isMethod(AstNode identifier) {
        return identifier.is(PlSqlGrammar.IDENTIFIER_NAME) && 
               "TO_DATE".equalsIgnoreCase(identifier.getTokenOriginalValue());
    }

    @Override
    protected void checkArguments(List<AstNode> arguments) {
        if (arguments.size() == 1) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), getCurrentNode());
        }
    }

}
