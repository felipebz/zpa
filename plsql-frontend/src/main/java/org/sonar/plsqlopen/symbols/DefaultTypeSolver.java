package org.sonar.plsqlopen.symbols;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType.Type;

import com.sonar.sslr.api.AstNode;

public class DefaultTypeSolver {

    public PlSqlType solve(AstNode node) {
        Type type = Type.UNKNOWN;
        if (node != null) {
            if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE)) {
                type = Type.CHARACTER;
            } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE)) {
                type = Type.NUMERIC;
            } else if (node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE)) {
                type = Type.DATE;
            } else if (node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE)) {
                type = Type.LOB;
            } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE)) {
                type = Type.BOOLEAN;
            }
        }
        return new PlSqlType(type, node);
    }

}
