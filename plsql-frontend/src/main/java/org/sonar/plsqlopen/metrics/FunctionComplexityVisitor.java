package org.sonar.plsqlopen.metrics;

import java.util.ArrayList;
import java.util.List;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;

public class FunctionComplexityVisitor extends ComplexityVisitor {
    
    private int numberOfFunctions;
    private List<Integer> functionComplexities = new ArrayList<>();

    private static final PlSqlGrammar[] METHOD_DECLARATION = { 
          PlSqlGrammar.CREATE_PROCEDURE,
          PlSqlGrammar.CREATE_FUNCTION, 
          PlSqlGrammar.PROCEDURE_DECLARATION,
          PlSqlGrammar.FUNCTION_DECLARATION };
            
    private int functionNestingLevel = 0;

    @Override
    public void visitNode(AstNode node) {
        if (node.is(METHOD_DECLARATION)) {
            functionNestingLevel++;
            numberOfFunctions++;
        }
        if (functionNestingLevel == 1) {
            super.visitNode(node);
        }
    }

    @Override
    public void leaveNode(AstNode node) {
        if (node.is(METHOD_DECLARATION)) {
            functionNestingLevel--;
            functionComplexities.add(getComplexity());
        }
    }
    
    public int getNumberOfFunctions() {
        return numberOfFunctions;
    }
    
    public List<Integer> getFunctionComplexities() {
        return functionComplexities;
    }
    
}
