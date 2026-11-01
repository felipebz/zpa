//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.config.internal.MapSettings

class PlSqlTest {

    @Test
    fun test() {
        val language = PlSql(MapSettings().asConfig())
        assertThat(language.key).isEqualTo("plsqlopen")
        assertThat(language.name).isEqualTo("PL/SQL (ZPA)")
        assertThat(language.fileSuffixes)
                .hasSize(4)
                .contains("sql")
                .contains("pkg")
                .contains("pks")
                .contains("pkb")
    }

    @Test
    fun custom_file_suffixes() {
        val settings = MapSettings()
        settings.setProperty(PlSqlPlugin.FILE_SUFFIXES_KEY, "sql, custom")

        val language = PlSql(settings.asConfig())
        assertThat(language.fileSuffixes).hasSize(2).contains("custom")
    }

}
