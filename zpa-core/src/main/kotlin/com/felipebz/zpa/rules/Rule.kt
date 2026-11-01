//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleInfo

class Rule(override val key: String) : ZpaRule {

    private val parameters =  mutableListOf<ZpaRuleParam>()

    override var htmlDescription: String = ""

    override var name: String = ""

    override val params: List<ZpaRuleParam> = parameters

    override var remediationConstant: String = ""
        set(value) {
            require(REMEDIATION_PATTERN.matches(value)) { "Invalid base effort: $value" }
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
