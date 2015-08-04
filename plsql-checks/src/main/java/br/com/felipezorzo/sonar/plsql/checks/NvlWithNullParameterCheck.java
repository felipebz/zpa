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
    key = NvlWithNullParameterCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class NvlWithNullParameterCheck extends BaseMethodCallChecker {
    public static final String CHECK_KEY = "NvlWithNullParameter";

    @Override
    protected boolean isMethod(AstNode identifier) {
        return identifier.is(PlSqlGrammar.IDENTIFIER_NAME) &&
               "NVL".equalsIgnoreCase(identifier.getTokenOriginalValue());
    }

    @Override
    protected void checkArguments(List<AstNode> arguments) {
        for (AstNode argument : arguments) {
            AstNode argumentValue = argument.getLastChild();
            if (CheckUtils.isNullLiteralOrEmptyString(argumentValue)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), getCurrentNode(), argumentValue.getTokenValue());
            }
        }
    }

}
