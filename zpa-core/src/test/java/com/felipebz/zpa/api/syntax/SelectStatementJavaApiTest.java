/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package com.felipebz.zpa.api.syntax;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SelectStatementJavaApiTest {

    @Test
    void exposesSelectStatementViewToJavaSubscribers() {
        assertThat(new JavaVisitor()).isNotNull();
    }

    private static class JavaVisitor extends com.felipebz.zpa.api.checks.PlSqlVisitor {

        @Override
        public void init() {
            subscribeTo(SyntaxViews.SELECT_STATEMENT, this::visitSelectStatement);
        }

        private void visitSelectStatement(SelectStatement statement) {
            statement.getSelectExpressionAstNode();

            for (SelectQueryBlock block : statement.getQueryBlocks()) {
                block.getSelectColumnAstNodes();
                SelectIntoClause into = block.getIntoClause();
                if (into != null) {
                    into.isBulkCollect();
                }
                block.getFromClauseAstNode();
                block.getWhereClauseAstNode();
                block.getGroupByClauseAstNode();
                block.getHavingClauseAstNode();
                block.getModelClauseAstNode();
            }

            statement.getHasSetOperation();
        }
    }
}
