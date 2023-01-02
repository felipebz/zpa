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
package org.sonar.plsqlopen.toolkit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class PlSqlConfigurationModelTest {

    @Test
    fun getConfiguration_charset() {
        val model = PlSqlConfigurationModel()
        model.charsetProperty.value = "UTF-8"
        assertThat(model.charset).isEqualTo(StandardCharsets.UTF_8)
        assertThat(model.configuration.charset).isEqualTo(StandardCharsets.UTF_8)
        model.charsetProperty.value = "ISO-8859-1"
        assertThat(model.charset).isEqualTo(StandardCharsets.ISO_8859_1)
        assertThat(model.configuration.charset).isEqualTo(StandardCharsets.ISO_8859_1)
    }

    @Test
    fun getPropertyOrDefaultValue_with_property_set() {
        val oldValue = System.getProperty("foo")

        try {
            System.setProperty("foo", "bar")
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("bar")
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo")
            } else {
                System.setProperty("foo", oldValue)
            }
        }
    }

    @Test
    fun getPropertyOrDefaultValue_with_property_not_set() {
        val oldValue = System.getProperty("foo")

        try {
            System.clearProperty("foo")
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("baz")
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo")
            } else {
                System.setProperty("foo", oldValue)
            }
        }
    }
}
