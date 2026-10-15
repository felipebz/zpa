/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.squid

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ConditionalCompilationSettingsTest {

    @Test
    fun parsesFlags() {
        assertThat(ConditionalCompilationSettings.parseFlags(" debug : TRUE ,level:2, off:false,n:Null, neg:-7 "))
            .containsExactly(
                java.util.Map.entry("DEBUG", CcValue.True),
                java.util.Map.entry("LEVEL", CcValue.Integer(2)),
                java.util.Map.entry("OFF", CcValue.False),
                java.util.Map.entry("N", CcValue.Null),
                java.util.Map.entry("NEG", CcValue.Integer(-7))
            )
        assertThat(ConditionalCompilationSettings.parseFlags("a:1, A:true")).containsExactly(java.util.Map.entry("A", CcValue.True))
        assertThat(ConditionalCompilationSettings.parseFlags("  ")).isEmpty()
    }

    @Test
    fun rejectsInvalidFlags() {
        listOf("a", "a:", ":1", "a:yes", "a:1.5", "a:1:2", "a:1,,b:2", "1a:1", "a b:1", "a:99999999999").forEach {
            assertThatThrownBy { ConditionalCompilationSettings.parseFlags(it) }.describedAs(it)
                .isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThatThrownBy { ConditionalCompilationSettings.parseFlags("debug:maybe") }
            .hasMessageContaining("debug").hasMessageContaining("maybe")
    }

    @Test
    fun parsesVersions() {
        assertThat(ConditionalCompilationSettings.parseVersion("19")).isEqualTo(DatabaseVersion(19, 0))
        assertThat(ConditionalCompilationSettings.parseVersion("19.0")).isEqualTo(DatabaseVersion(19, 0))
        assertThat(ConditionalCompilationSettings.parseVersion(" 12.2 ")).isEqualTo(DatabaseVersion(12, 2))
        assertThat(ConditionalCompilationSettings.parseVersion("23.4")).isEqualTo(DatabaseVersion(23, 4))
        listOf("", "x", "19.", "19.1.2", "-1", "19c").forEach {
            assertThatThrownBy { ConditionalCompilationSettings.parseVersion(it) }.describedAs(it)
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun activity() {
        assertThat(ConditionalCompilationSettings.NONE.isActive).isFalse()
        assertThat(ConditionalCompilationSettings.of("", " ").isActive).isFalse()
        assertThat(ConditionalCompilationSettings.of("a:1", null).isActive).isTrue()
        assertThat(ConditionalCompilationSettings.of(null, "19").isActive).isTrue()
        assertThat(ConditionalCompilationSettings(mapOf("debug" to CcValue.True)).flags).containsKey("DEBUG")
    }
}
