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
