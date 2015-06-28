package br.com.felipezorzo.sonar.plsql.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.ast.AstSelect;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;

@Rule(
    key = EmptyBlockCheck.CHECK_KEY,
    priority = Priority.INFO,
    name = "Empty blocks should be removed.",
    tags = Tags.UNUSED
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
@ActivatedByDefault
public class EmptyBlockCheck extends SquidCheck<Grammar> {
    public static final String CHECK_KEY = "P001";
    private static final String MESSAGE = "Either remove or fill this block of code.";
    
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
      
      getContext().createLineViolation(this, MESSAGE, stmtLists.get(0));
    }
}
