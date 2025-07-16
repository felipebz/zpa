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
package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleInfo

class Rule(override val key: String) : ZpaRule {

    private val parameters =  mutableListOf<ZpaRuleParam>()

    override var htmlDescription: String = ""

    override var name: String = ""

    override val params: List<ZpaRuleParam> = parameters

    override var remediationConstant: String = ""
        set(value) {
            if (!REMEDIATION_PATTERN.matches(value)) {
                throw IllegalArgumentException("Invalid base effort: $value")
            }
            field = value
        }

    override var scope: RuleInfo.Scope = RuleInfo.Scope.ALL

    override var severity: String = ""

    override var status: RuleStatus = RuleStatus.READY

    override var tags: Array<String> = emptyArray()

    override var template: Boolean = false

    override var isActivatedByDefault = false

    override fun createParam(fieldKey: String): ZpaRuleParam {
        val param = RuleParam(fieldKey)
        parameters.add(param)
        return param
    }

    companion object {
        private const val DAY = "d"
        private const val HOUR = "h"
        private const val MINUTE = "min"
        private val REMEDIATION_PATTERN = Regex("\\s*+(?:(\\d++)\\s*+$DAY)?+\\s*+(?:(\\d++)\\s*+$HOUR)?+\\s*+(?:(\\d++)\\s*+$MINUTE)?+\\s*+")
    }
}
