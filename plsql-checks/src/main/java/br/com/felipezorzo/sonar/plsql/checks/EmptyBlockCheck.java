package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.sslr.ast.AstSelect;

import com.sonar.sslr.api.AstNode;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

@Rule(
    key = EmptyBlockCheck.CHECK_KEY,
    priority = Priority.MINOR,
    tags = Tags.UNUSED
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class EmptyBlockCheck extends BaseCheck {
    public static final String CHECK_KEY = "EmptyBlock";
    
    @Override
    public void init() {
      subscribeTo(PlSqlGrammar.BLOCK_STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode suiteNode) {
      
      AstSelect suite = suiteNode.select();
      AstSelect stmtLists = suite.children(PlSqlGrammar.STATEMENT);
      if (stmtLists.size() != 1) return;
      
      AstSelect nullStatementSelect = stmtLists.children(PlSqlGrammar.NULL_STATEMENT);
      if (nullStatementSelect.isEmpty()) return;
      
      getContext().createLineViolation(this, getLocalizedMessage(CHECK_KEY), stmtLists.get(0));
    }
}
