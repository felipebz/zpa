/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2021 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.plsqlopen.api.matchers

import java.util.*

@FunctionalInterface
interface NameCriteria {

    fun matches(name: String): Boolean

    companion object {

        @JvmStatic
        fun any(): NameCriteria =
            object : NameCriteria {
                override fun matches(name: String) = true
            }

        @JvmStatic
        fun `is`(exactName: String): NameCriteria =
            object : NameCriteria {
                override fun matches(name: String) = exactName.equals(name, ignoreCase = true)
            }

        @JvmStatic
        fun startsWith(prefix: String): NameCriteria =
            object : NameCriteria {
                override fun matches(name: String) = name.uppercase(Locale.getDefault())
                    .startsWith(prefix.uppercase(Locale.getDefault()))
            }

        @JvmStatic
        fun `in`(vararg prefix: String): NameCriteria =
            object : NameCriteria {
                override fun matches(name: String) = prefix.any { name.equals(it, ignoreCase = true) }
            }

    }

}
