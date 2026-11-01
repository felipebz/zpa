//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

interface ZpaActiveRule {
    val ruleKey: ZpaRuleKey
    val severity: String
    val language: String
    fun param(key: String): String?
    val params: Map<String, String>
    val internalKey: String?
    val templateRuleKey: String?
    val tags: Array<String>
    val remediationConstant: String
    val name: String
}
