/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
package org.sonar.plsqlopen.checks;

import java.util.List;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sonar.sslr.api.AstNode;

@Rule(
    key = UnnecessaryAliasInQueryCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("1min")
public class UnnecessaryAliasInQueryCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "UnnecessaryAliasInQuery";

    private static final int DEFAULT_ACCEPTED_LENGTH = 3;
    
    @RuleProperty(
        key = "acceptedLength",
        defaultValue = "" + DEFAULT_ACCEPTED_LENGTH)
    public int acceptedLength = DEFAULT_ACCEPTED_LENGTH;
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.SELECT_EXPRESSION);
    }

    @Override
    public void visitNode(AstNode node) {
        if (node.hasAncestor(PlSqlGrammar.SELECT_EXPRESSION)) {
            return;
        }
        
        ListMultimap<String, TableReference> tableReferences = ArrayListMultimap.create();
        for (AstNode fromClause : node.getDescendants(PlSqlGrammar.FROM_CLAUSE)) {
            AstNode table = fromClause.getFirstChild(PlSqlGrammar.TABLE_REFERENCE);
            AstNode alias = fromClause.getFirstChild(PlSqlGrammar.ALIAS);

            
            if (table != null) {
                tableReferences.put(table.getTokenOriginalValue().toLowerCase(),
                        new TableReference(table, alias));
            }
        }
        
        for (String tableName : tableReferences.keySet()) {
            List<TableReference> references = tableReferences.get(tableName);
            checkReference(references);
        }
    }

    private void checkReference(List<TableReference> references) {
        if (references.size() == 1) {
            TableReference reference = references.get(0);
            
            String alias = null;
            if (reference.alias != null) {
                alias = reference.alias.getTokenOriginalValue();
            }
            
            if (alias != null && alias.length() < acceptedLength) {
                getPlSqlContext().createViolation(this, getLocalizedMessage(CHECK_KEY), 
                        reference.table,
                        reference.table.getTokenOriginalValue(),
                        reference.alias.getTokenOriginalValue());
            }
        }
    }
    
    class TableReference {
        public AstNode table;
        public AstNode alias;
        
        public TableReference(AstNode table, AstNode alias) {
            this.table = table;
            this.alias = alias;
        }
    }

}
