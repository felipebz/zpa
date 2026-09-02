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
