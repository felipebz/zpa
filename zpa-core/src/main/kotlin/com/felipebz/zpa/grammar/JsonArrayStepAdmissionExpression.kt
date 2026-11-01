//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.grammar

import com.felipebz.flr.internal.vm.Machine
import com.felipebz.flr.internal.vm.NativeExpression
import com.felipebz.zpa.api.PlSqlPunctuator.DOT
import com.felipebz.zpa.api.PlSqlPunctuator.LBRACKET

/**
 * Admits JSON object access without using PEG lookahead, which would reparse
 * the very common qualified-name prefix and caused a significant parser
 * performance regression. Only the punctuation positions that may introduce
 * a JSON array step are inspected; complete JSON syntax validation remains in
 * `JSON_OBJECT_ACCESS_EXPRESSION`.
 */
public object JsonArrayStepAdmissionExpression : NativeExpression() {
    override fun execute(machine: Machine) {
        val directArrayStep = machine.length > 3 && machine.tokenAt(3).type == LBRACKET
        val nestedArrayStep = machine.length > 5
            && machine.tokenAt(3).type == DOT
            && machine.tokenAt(5).type == LBRACKET

        if (directArrayStep || nestedArrayStep) {
            machine.jump(1)
        } else {
            machine.backtrack()
        }
    }
}
