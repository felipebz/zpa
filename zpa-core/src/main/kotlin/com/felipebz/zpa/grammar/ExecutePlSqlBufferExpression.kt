//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
