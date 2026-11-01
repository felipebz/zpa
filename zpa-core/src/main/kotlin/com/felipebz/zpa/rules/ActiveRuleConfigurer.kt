//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

fun interface ActiveRuleConfigurer {
    fun apply(repo: Repository, rule: ZpaRule, configuration: ActiveRuleConfiguration): Boolean
}
