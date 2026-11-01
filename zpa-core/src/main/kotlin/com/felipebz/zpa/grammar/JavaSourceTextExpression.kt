//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.grammar

import com.felipebz.flr.api.GenericTokenType
import com.felipebz.flr.internal.matchers.Matcher
import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression

object JavaSourceTextExpression : NativeExpression(), Matcher {
    override fun execute(machine: Machine) {
        var offset = 0
        while (offset < machine.length) {
            if (machine.tokenAt(offset).type == GenericTokenType.EOF
                || isExecuteBufferDelimiter(machine, offset)) {
                break
            }
            offset++
        }

        machine.createLeafNode(this, offset)
        machine.jump(1)
    }

    override fun toString(): String {
        return "JavaSourceText"
    }
}
