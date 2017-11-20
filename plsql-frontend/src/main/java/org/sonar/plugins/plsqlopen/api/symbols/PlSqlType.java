package org.sonar.plugins.plsqlopen.api.symbols;

import com.google.common.annotations.VisibleForTesting;
import com.sonar.sslr.api.AstNode;

public class PlSqlType {
    
    private Type type;
    private AstNode node;
    
    public PlSqlType(Type type) {
        this(type, null);
    }

    public PlSqlType(Type type, AstNode node) {
        this.type = type;
        this.node = node;
    }
    
    public boolean isCharacter() {
        return type == Type.CHARACTER;
    }
    
    public boolean isNumeric() {
        return type == Type.NUMERIC;
    }
    
    public boolean isUnknown() {
        return type == Type.UNKNOWN;
    }
    
    public AstNode node() {
        return node;
    }
    
    @VisibleForTesting
    public Type type() {
        return type;
    }
    
    @Override
    public String toString() {
        return "PlSqlType{" +
                "type=" + type +
                ", node=" + node + "}";
    }
    
    public enum Type {
        UNKNOWN,
        CHARACTER,
        NUMERIC,
        DATE,
        LOB,
        BOOLEAN
    }

}
