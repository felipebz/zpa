//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.matchers

import java.util.*

fun interface NameCriteria {

    fun matches(name: String?): Boolean

    companion object {

        @JvmStatic
        fun any(): NameCriteria =
            NameCriteria { true }

        @JvmStatic
        fun `is`(exactName: String): NameCriteria =
            NameCriteria { name -> exactName.equals(name, ignoreCase = true) }

        @JvmStatic
        fun startsWith(prefix: String): NameCriteria =
            NameCriteria { name ->
                name != null &&
                    name.uppercase(Locale.getDefault())
                        .startsWith(prefix.uppercase(Locale.getDefault()))
            }

        @JvmStatic
        fun `in`(vararg prefix: String): NameCriteria =
            NameCriteria { name -> prefix.any { name.equals(it, ignoreCase = true) } }

    }

}
