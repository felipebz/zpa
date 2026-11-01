//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.rules

class RuleKey(override val repository: String, private val key: String) : ZpaRuleKey {

    override val rule: String
        get() = key

    override fun toString(): String = "$repository:$key"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as RuleKey
        return this.key == other.key && this.repository == other.repository
    }

    override fun hashCode(): Int {
        var result = repository.hashCode()
        result = 31 * result + key.hashCode()
        return result
    }

}
