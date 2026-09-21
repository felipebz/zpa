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
package com.felipebz.zpa.grammar

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.internal.matchers.Matcher
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.api.PlSqlPunctuator

object ExecuteBufferExpression : NativeExpression(), Matcher {
    override fun execute(machine: Machine) {
        if (isExecuteBufferDelimiter(machine, 0)) {
            machine.createLeafNode(this, 1)
            machine.jump(1)
        } else {
            machine.backtrack()
        }
    }

    override fun toString(): String {
        return "ExecuteBuffer"
    }
}

internal fun isExecuteBufferDelimiter(machine: Machine, offset: Int): Boolean {
    if (machine.length <= offset + 1) {
        return false
    }

    val previousTokenLine = if (offset == 0) {
        if (machine.index == 0) 0 else machine.tokenAt(-1).line
    } else {
        machine.tokenAt(offset - 1).line
    }
    val token = machine.tokenAt(offset)
    val nextToken = machine.tokenAt(offset + 1)

    return token.type == PlSqlPunctuator.DIVISION
        && (token.line != previousTokenLine || previousTokenLine == 0)
        && (token.line != nextToken.line || nextToken.type == GenericTokenType.EOF)
}
