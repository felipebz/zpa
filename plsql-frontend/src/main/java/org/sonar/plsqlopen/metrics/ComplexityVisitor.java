package org.sonar.plsqlopen.metrics;

import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;

import com.sonar.sslr.api.AstNode;

public class ComplexityVisitor extends PlSqlVisitor {

    private int complexity;
    
    @Override
    public void init() {
        subscribeTo(
            PlSqlGrammar.CREATE_PROCEDURE,
            PlSqlGrammar.CREATE_FUNCTION,
            PlSqlGrammar.ANONYMOUS_BLOCK,
            
            PlSqlGrammar.PROCEDURE_DECLARATION,
            PlSqlGrammar.FUNCTION_DECLARATION,
            
            PlSqlGrammar.LOOP_STATEMENT,
            PlSqlGrammar.CONTINUE_STATEMENT,
            PlSqlGrammar.FOR_STATEMENT,
            PlSqlGrammar.EXIT_STATEMENT,
            PlSqlGrammar.IF_STATEMENT,
            PlSqlGrammar.RAISE_STATEMENT,
            PlSqlGrammar.RETURN_STATEMENT,
            PlSqlGrammar.WHILE_STATEMENT,
            
            // this includes WHEN in exception handlers, exit/continue statements and CASE expressions
            PlSqlKeyword.WHEN,
            PlSqlKeyword.ELSIF);
    }

    @Override
    public void visitFile(AstNode node) {
        complexity = 0;
    }

    @Override
    public void visitNode(AstNode node) {
        complexity++;
    }

    public int getComplexity() {
        return complexity;
    }
    
}
