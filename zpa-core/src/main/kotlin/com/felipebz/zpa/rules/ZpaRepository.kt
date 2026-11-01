//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

interface ZpaRepository {
    val key: String
    fun createRule(ruleKey: String): ZpaRule
    fun rule(ruleKey: String): ZpaRule?
}
