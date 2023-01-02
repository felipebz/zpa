/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2023 Felipe Zorzo
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
package org.sonar.plsqlopen

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
