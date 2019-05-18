/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen

import org.sonar.api.config.Configuration
import org.sonar.api.resources.AbstractLanguage

class PlSql internal constructor(private val settings: Configuration) : AbstractLanguage(KEY, "PL/SQL (ZPA)") {

    private val defaultFileSuffixes = arrayOf("sql", "pkg", "pks", "pkb")

    override fun getFileSuffixes(): Array<String> {
        val suffixes = filterEmptyStrings(settings.getStringArray(PlSqlPlugin.FILE_SUFFIXES_KEY))
        return if (suffixes.isEmpty()) defaultFileSuffixes else suffixes
    }

    private fun filterEmptyStrings(stringArray: Array<String>): Array<String> {
        val nonEmptyStrings = stringArray
            .filterNot { it.isBlank() }
            .map { it.trim() }
        return nonEmptyStrings.toTypedArray()
    }

    companion object {
        const val KEY = "plsqlopen"
    }

}
