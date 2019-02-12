/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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
package org.sonar.plsqlopen.symbols;

import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.PlSqlTokenType;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;

import com.sonar.sslr.api.AstNode;

public class DefaultTypeSolver {

    public PlSqlType solve(AstNode node) {
        if (node != null) {
            if (node.getType() == PlSqlGrammar.DATATYPE) {
                return solveDatatype(node);
            }
            else if (node.getType() == PlSqlGrammar.LITERAL) {
                return solveLiteral(node);
            }
        }
        return PlSqlType.UNKNOWN;
    }

    private PlSqlType solveDatatype(AstNode node) {
        PlSqlType type = PlSqlType.UNKNOWN;
        if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_DATAYPE)) {
            type = PlSqlType.CHARACTER;
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_DATATYPE)) {
            type = PlSqlType.NUMERIC;
        } else if (node.hasDirectChildren(PlSqlGrammar.DATE_DATATYPE)) {
            type = PlSqlType.DATE;
        } else if (node.hasDirectChildren(PlSqlGrammar.LOB_DATATYPE)) {
            type = PlSqlType.LOB;
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_DATATYPE)) {
            type = PlSqlType.BOOLEAN;
        } else {
            AstNode anchoredDatatype = node.getFirstChild(PlSqlGrammar.ANCHORED_DATATYPE);
            if (anchoredDatatype != null && anchoredDatatype.getLastChild().getType() == PlSqlKeyword.ROWTYPE) {
                type = PlSqlType.ROWTYPE;
            }
        }
        return type;
    }

    private PlSqlType solveLiteral(AstNode node) {
        PlSqlType type = PlSqlType.UNKNOWN;
        if (node.hasDirectChildren(PlSqlGrammar.NULL_LITERAL) || isEmptyString(node)) {
            type = PlSqlType.NULL;
        } else if (node.hasDirectChildren(PlSqlGrammar.CHARACTER_LITERAL)) {
            type = PlSqlType.CHARACTER;
        } else if (node.hasDirectChildren(PlSqlGrammar.NUMERIC_LITERAL)) {
            type = PlSqlType.NUMERIC;
        } else if (node.hasDirectChildren(PlSqlTokenType.DATE_LITERAL)) {
            type = PlSqlType.DATE;
        } else if (node.hasDirectChildren(PlSqlGrammar.BOOLEAN_LITERAL)) {
            type = PlSqlType.BOOLEAN;
        }
        return type;
    }


    // TODO: this was duplicated from org.sonar.plsqlopen.checks.CheckUtils#isEmptyString, it needs to be moved to a better place
    private static boolean isEmptyString(AstNode node) {
        AstNode characterLiteral = node.getFirstChild(PlSqlGrammar.CHARACTER_LITERAL);
        return characterLiteral != null && "''".equals(characterLiteral.getTokenValue());
    }

}
