//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleProperty

class RulePropertyData(val key: String,
                       val description: String,
                       val defaultValue: String,
                       val type: String) {
    companion object {
        fun from(ruleProperty: RuleProperty?) =
            if (ruleProperty == null) null
            else
                RulePropertyData(ruleProperty.key,
                    ruleProperty.description,
                    ruleProperty.defaultValue,
                    ruleProperty.type)
    }
}
