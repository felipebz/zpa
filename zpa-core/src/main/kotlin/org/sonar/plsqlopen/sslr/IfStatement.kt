/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package org.sonar.plsqlopen.sslr

import org.sonar.plsqlopen.asSemantic
import org.sonar.plsqlopen.asTree
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode

class IfStatement(override val astNode: SemanticAstNode) : TreeWithStatements(astNode) {

    val condition : SemanticAstNode by lazy {
        astNode.children[1].asSemantic()
    }

    val elsifClauses : List<ElsifClause> by lazy {
        astNode.getChildren(PlSqlGrammar.ELSIF_CLAUSE).asTree()
    }

    val elseClause : ElseClause? by lazy {
        astNode.getFirstChildOrNull(PlSqlGrammar.ELSE_CLAUSE)?.asTree()
    }

}
