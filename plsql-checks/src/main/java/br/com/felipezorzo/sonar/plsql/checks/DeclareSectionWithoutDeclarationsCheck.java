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
    key = DeclareSectionWithoutDeclarationsCheck.CHECK_KEY,
    priority = Priority.INFO
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("1min")
@ActivatedByDefault
public class DeclareSectionWithoutDeclarationsCheck extends BaseCheck {
    public static final String CHECK_KEY = "DeclareSectionWithoutDeclarations";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.BLOCK_STATEMENT);
    }

    @Override
    public void visitNode(AstNode node) {
        if (node.hasDirectChildren(PlSqlKeyword.DECLARE) && !node.hasDescendant(PlSqlGrammar.DECLARE_SECTION)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
        }
    }
}
