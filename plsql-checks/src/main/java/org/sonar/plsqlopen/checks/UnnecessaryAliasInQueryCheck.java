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
package org.sonar.plsqlopen.checks;

import java.util.List;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.plsqlopen.api.DmlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plsqlopen.annnotations.ConstantRemediation;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

@Rule(
    key = UnnecessaryAliasInQueryCheck.CHECK_KEY,
    priority = Priority.MINOR
)
@ConstantRemediation("1min")
public class UnnecessaryAliasInQueryCheck extends AbstractBaseCheck {
    public static final String CHECK_KEY = "UnnecessaryAliasInQuery";

    private static final int DEFAULT_ACCEPTED_LENGTH = 3;
    
    @RuleProperty(
        key = "acceptedLength",
        defaultValue = "" + DEFAULT_ACCEPTED_LENGTH)
    public int acceptedLength = DEFAULT_ACCEPTED_LENGTH;
    
    private AstNodeType[] dmlStatements = new AstNodeType[] {
            DmlGrammar.SELECT_EXPRESSION,
            PlSqlGrammar.UPDATE_STATEMENT,
            PlSqlGrammar.DELETE_STATEMENT
    };
    
    @Override
    public void init() {
        subscribeTo(dmlStatements);
    }

    @Override
    public void visitNode(AstNode node) {
        if (node.hasAncestor(dmlStatements)) {
            // if the current node is inside another DML statement (i.e. subquery), the node should be
            // ignored because it is considered in the analysis of the outer statement 
            return;
        }
        
        ListMultimap<String, TableReference> tableReferences = ArrayListMultimap.create();
        for (AstNode fromClause : node.getDescendants(DmlGrammar.DML_TABLE_EXPRESSION_CLAUSE)) {
            AstNode table = fromClause.getFirstChild(DmlGrammar.TABLE_REFERENCE);
            AstNode alias = fromClause.getFirstChild(DmlGrammar.ALIAS);

            
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
                getContext().createViolation(this, getLocalizedMessage(CHECK_KEY), 
                        reference.alias,
                        reference.table.getTokenOriginalValue(),
                        reference.alias.getTokenOriginalValue());
            }
        }
    }
    
    class TableReference {
        public final AstNode table;
        public final AstNode alias;
        
        public TableReference(AstNode table, AstNode alias) {
            this.table = table;
            this.alias = alias;
        }
    }

}
