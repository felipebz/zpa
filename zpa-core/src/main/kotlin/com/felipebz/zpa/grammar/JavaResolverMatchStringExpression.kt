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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.zpa.grammar

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.internal.matchers.Matcher
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.api.PlSqlPunctuator

object JavaResolverMatchStringExpression : NativeExpression(), Matcher {
    override fun execute(machine: Machine) {
        if (machine.length > 0) {
            val token = machine.tokenAt(0)
            val originalValue = token.originalValue
            val quotedIdentifier = token.type == GenericTokenType.IDENTIFIER
                && originalValue.length >= 2
                && originalValue.first() == '"'
                && originalValue.last() == '"'
            if (token.type == PlSqlPunctuator.MULTIPLICATION || quotedIdentifier) {
                machine.createLeafNode(this, 1)
                machine.jump(1)
                return
            }
        }

        machine.backtrack()
    }

    override fun toString(): String {
        return "JavaResolverMatchString"
    }
}
