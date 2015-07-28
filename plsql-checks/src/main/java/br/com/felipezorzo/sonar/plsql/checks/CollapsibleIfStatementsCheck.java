package br.com.felipezorzo.sonar.plsql.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.sslr.ast.AstSelect;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;

@Rule(
    key = CollapsibleIfStatementsCheck.CHECK_KEY,
    priority = Priority.MAJOR,
    tags = Tags.CLUMSY
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class CollapsibleIfStatementsCheck extends BaseCheck {

    public static final String CHECK_KEY = "CollapsibleIfStatements";

    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.IF_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        AstNode singleIfChild = singleIfChild(node);
        if (singleIfChild != null && !hasElseOrElif(singleIfChild)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), singleIfChild);
        }
    }

    private boolean hasElseOrElif(AstNode ifNode) {
        return ifNode.hasDirectChildren(PlSqlKeyword.ELSIF) || ifNode.hasDirectChildren(PlSqlKeyword.ELSE);
    }

    private AstNode singleIfChild(AstNode suite) {
        List<AstNode> statements = suite.getChildren(PlSqlGrammar.STATEMENT);
        if (statements.size() != 1) return null;
        
        AstSelect nestedIf = statements.get(0).select().children(PlSqlGrammar.IF_STATEMENT);
        if (nestedIf.size() != 1) return null;
            
        return nestedIf.get(0);
    }

}
