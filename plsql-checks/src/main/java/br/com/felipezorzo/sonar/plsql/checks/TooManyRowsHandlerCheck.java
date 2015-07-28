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
    key = TooManyRowsHandlerCheck.CHECK_KEY,
    priority = Priority.CRITICAL
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class TooManyRowsHandlerCheck extends BaseCheck {
    public static final String CHECK_KEY = "TooManyRowsHandler";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.EXCEPTION_HANDLER);
    }

    @Override
    public void visitNode(AstNode node) {
        // is a TOO_MANY_ROWS handler
        AstNode exceptionName = node.getChildren().get(1);
        if (!exceptionName.is(PlSqlGrammar.IDENTIFIER_NAME)) return;  
        if (!exceptionName.getTokenValue().equalsIgnoreCase("too_many_rows")) return;
    
        // and have only one NULL_STATEMENT
        List<AstNode> children = node.getChildren(PlSqlGrammar.STATEMENT);
        if (children.size() > 1) return;
        if (children.get(0).getFirstChild().is(PlSqlGrammar.NULL_STATEMENT)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
        }
        
        
    }
}

