//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class ActiveRuleConfiguration(
    val repositoryKey: String,
    val key: String,
    var severity: String? = null,
    var parameters: MutableMap<String, String> = mutableMapOf()
) {
    fun keyIs(repositoryKey: String, key: String): Boolean {
        return this.repositoryKey == repositoryKey && this.key == key
    }
}
