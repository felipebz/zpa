//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import org.sonar.api.config.Configuration
import org.sonar.api.resources.AbstractLanguage

class PlSql internal constructor(private val settings: Configuration) : AbstractLanguage(KEY, "PL/SQL (ZPA)") {

    private val defaultFileSuffixes = arrayOf("sql", "pkg", "pks", "pkb")

    override fun getFileSuffixes(): Array<String> {
        val suffixes = filterEmptyStrings(settings.getStringArray(PlSqlPlugin.Companion.FILE_SUFFIXES_KEY))
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
