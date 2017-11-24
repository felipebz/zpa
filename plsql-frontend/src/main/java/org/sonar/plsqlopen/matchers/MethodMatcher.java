/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.matchers;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

public class MethodMatcher {

    private static final AstNodeType[] VARIABLE_OR_IDENTIFIER = { PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME };
    
    private NameCriteria methodNameCriteria;
    private NameCriteria packageNameCriteria;
    private NameCriteria schemaNameCriteria;
    private int parameterCount;
    private boolean shouldCheckParameters = true;
    private boolean schemaIsOptional = false;
    private String methodName;

    private MethodMatcher() {
        // instances should be created using the create method
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public static MethodMatcher create() {
        return new MethodMatcher();
    }
    
    public MethodMatcher name(String methodNameCriteria) {
        return name(NameCriteria.is(methodNameCriteria));
    }
    
    public MethodMatcher name(NameCriteria methodNameCriteria) {
        Preconditions.checkState(this.methodNameCriteria == null);
        this.methodNameCriteria = methodNameCriteria;
        return this;
    }
    
    public MethodMatcher packageName(String packageNameCriteria) {
        return packageName(NameCriteria.is(packageNameCriteria));
    }
    
    public MethodMatcher packageName(NameCriteria packageNameCriteria) {
        Preconditions.checkState(this.packageNameCriteria == null);
        this.packageNameCriteria = packageNameCriteria;
        return this;
    }
    
    public MethodMatcher schema(String schemaNameCriteria) {
        return schema(NameCriteria.is(schemaNameCriteria));
    }
    
    public MethodMatcher schema(NameCriteria schemaNameCriteria) {
        Preconditions.checkState(this.schemaNameCriteria == null);
        this.schemaNameCriteria = schemaNameCriteria;
        return this;
    }
    
    public MethodMatcher withNoParameterConstraint() {
        Preconditions.checkState(this.parameterCount == 0);
        this.shouldCheckParameters = false;
        return this;
    }

    public MethodMatcher schemaIsOptional() {
        this.schemaIsOptional = true;
        return this;
    }
    
    public MethodMatcher addParameter() {
        Preconditions.checkState(this.shouldCheckParameters);
        this.parameterCount++;
        return this;
    }
    
    public MethodMatcher addParameters(int quantity) {
        Preconditions.checkState(this.shouldCheckParameters);
        this.parameterCount += quantity;
        return this;
    }
    
    public List<AstNode> getArguments(AstNode node) {
        AstNode arguments = node.getFirstChild(PlSqlGrammar.ARGUMENTS);
        if (arguments != null) {
            return arguments.getChildren(PlSqlGrammar.ARGUMENT);
        }
        
        return new ArrayList<>();
    }
    
    public List<AstNode> getArgumentsValues(AstNode node) {
        return getArguments(node).stream().map(AstNode::getLastChild).collect(toList());
    }

    public boolean matches(AstNode originalNode) {
        AstNode node = normalize(originalNode);
        LinkedList<AstNode> nodes = Lists.newLinkedList(node.getChildren(VARIABLE_OR_IDENTIFIER));
        
        if (nodes.isEmpty()) {
            return false;
        }
        
        boolean matches = true;
        
        matches &= nameAcceptable(nodes.removeLast(), methodNameCriteria);
        
        if (packageNameCriteria != null) {
            matches &= !nodes.isEmpty() && nameAcceptable(nodes.removeLast(), packageNameCriteria);
        }
        
        if (schemaNameCriteria != null) {
            matches &= (schemaIsOptional && nodes.isEmpty()) || 
                    (!nodes.isEmpty() && nameAcceptable(nodes.removeLast(), schemaNameCriteria));
        }
        
        return matches && nodes.isEmpty() && argumentsAcceptable(originalNode);
    }

    private boolean nameAcceptable(AstNode node, NameCriteria criteria) {
        methodName = node.getTokenOriginalValue();
        return criteria.matches(methodName);
    }

    private boolean argumentsAcceptable(AstNode node) {
        return !shouldCheckParameters || getArguments(node).size() == parameterCount;
    }
    
    private static AstNode normalize(AstNode node) {
        if (node.getType() == PlSqlGrammar.METHOD_CALL || node.getType() == PlSqlGrammar.CALL_STATEMENT) {
            AstNode child = normalize(node.getFirstChild());
            if (child.getFirstChild().getType() == PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE) {
                child = child.getFirstChild();
            }
            return child;
        }
        return node;
    }
    
}
