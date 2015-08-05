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
    key = ExplicitInParameterCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("2min")
@ActivatedByDefault
public class ExplicitInParameterCheck extends BaseCheck {
    public static final String CHECK_KEY = "ExplicitInParameter";
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.PARAMETER_DECLARATION);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (!node.hasDirectChildren(PlSqlKeyword.IN, PlSqlKeyword.OUT)) {
            getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), node);
        }
    }

}
