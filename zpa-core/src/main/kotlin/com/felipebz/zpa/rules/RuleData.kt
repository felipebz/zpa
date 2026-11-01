//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.Priority
import com.felipebz.zpa.api.annotations.Rule

class RuleData(val key: String,
               val name: String,
               val description: String,
               val priority: Priority,
               val tags: Array<String>,
               val status: String) {
    companion object {
        fun from(rule: Rule?): RuleData? =
            if (rule == null) null
            else RuleData(rule.key,
                rule.name,
                rule.description,
                rule.priority,
                rule.tags,
                rule.status)
    }
}
