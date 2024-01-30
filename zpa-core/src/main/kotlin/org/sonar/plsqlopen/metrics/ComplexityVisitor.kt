/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.metrics

import com.felipebz.flr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck

open class ComplexityVisitor : PlSqlCheck() {

    var complexity: Int = 0
        private set

    override fun init() {
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
                PlSqlKeyword.ELSIF)
    }

    override fun visitFile(node: AstNode) {
        complexity = 0
    }

    override fun visitNode(node: AstNode) {
        complexity++
    }

}
