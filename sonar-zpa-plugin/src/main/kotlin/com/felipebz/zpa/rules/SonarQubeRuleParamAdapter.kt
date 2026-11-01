//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import org.sonar.api.server.rule.RulesDefinition

class SonarQubeRuleParamAdapter(private val param: RulesDefinition.NewParam) : ZpaRuleParam {
    override val key: String
        get() = param.key()

    override var description: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            param.setDescription(value)
        }

    override var defaultValue: String
        get() = throw IllegalAccessException("Getter is not available")
        set(value) {
            param.setDefaultValue(value)
        }

}
