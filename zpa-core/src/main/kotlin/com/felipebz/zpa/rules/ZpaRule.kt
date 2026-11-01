//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

import com.felipebz.zpa.api.annotations.RuleInfo

interface ZpaRule {
    val key: String
    val params: List<ZpaRuleParam>
    var name: String
    var remediationConstant: String
    var scope: RuleInfo.Scope
    var template: Boolean
    var tags: Array<String>
    var status: RuleStatus
    var severity: String
    var htmlDescription: String
    var isActivatedByDefault: Boolean
    fun createParam(fieldKey: String): ZpaRuleParam
}
