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
package org.sonar.plugins.plsqlopen.api.symbols.datatype

import com.felipebz.flr.api.AstNode
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType
import org.sonar.plugins.plsqlopen.api.symbols.Scope

class AssociativeArrayDatatype(node: AstNode? = null, currentScope: Scope?, val nestedType: PlSqlDatatype) : PlSqlDatatype {
    override val type = PlSqlType.ASSOCIATIVE_ARRAY

    override val name: String = currentScope?.let {
        if (it.identifier != null && it.type == PlSqlGrammar.CREATE_PACKAGE)
            it.identifier + "."
        else "" } +
        node?.getFirstChild(PlSqlGrammar.IDENTIFIER_NAME)?.tokenValue

    override fun toString(): String {
        return "AssociativeArray{nestedType=$nestedType}"
    }
}
