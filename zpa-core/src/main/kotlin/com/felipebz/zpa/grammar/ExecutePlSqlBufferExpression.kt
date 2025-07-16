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
package com.felipebz.zpa.grammar

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.internal.matchers.Matcher
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.api.PlSqlPunctuator

public object ExecuteBufferExpression : NativeExpression(), Matcher {
    override fun execute(machine: Machine) {
        if (machine.length < 2) {
            machine.backtrack()
            return
        }

        val previousTokenLine = if (machine.index == 0) 0 else machine.tokenAt(-1).line
        val token = machine.tokenAt(0)
        val nextToken = machine.tokenAt(1)

        if (token.type == PlSqlPunctuator.DIVISION && token.column == 0
            && (token.line != previousTokenLine || previousTokenLine == 0)
            && (token.line != nextToken.line || nextToken.type == GenericTokenType.EOF)
        ) {
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
