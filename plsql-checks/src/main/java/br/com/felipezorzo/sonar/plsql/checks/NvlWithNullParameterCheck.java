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

@Rule(
    key = NvlWithNullParameterCheck.CHECK_KEY,
    priority = Priority.BLOCKER,
    tags = Tags.BUG
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class NvlWithNullParameterCheck extends BaseCheck {
    public static final String CHECK_KEY = "NvlWithNullParameter";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.METHOD_CALL);
    }
    
    @Override
    public void visitNode(AstNode node) {
        AstNode identifier = node.getFirstChild();
        if (!identifier.is(PlSqlGrammar.IDENTIFIER_NAME)) return;
        if (!identifier.getTokenOriginalValue().equalsIgnoreCase("NVL")) return;
        
        AstNode arguments = node.getFirstChild(PlSqlGrammar.ARGUMENTS);
        if (arguments == null) return;
        
        List<AstNode> allArguments = arguments.getChildren(PlSqlGrammar.ARGUMENT);
        for (AstNode argument : allArguments) {
            AstNode argumentValue = argument.getLastChild();
            if (CheckUtils.isNullLiteralOrEmptyString(argumentValue)) {
                getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
            }
        }
    }

}
