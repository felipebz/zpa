/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
import org.sonar.api.Plugin
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.Version

class PlSqlPluginTest {

    @Test
    fun testGetExtensions() {
        val context = Plugin.Context(SonarRuntimeImpl.forSonarQube(Version.create(6, 0), SonarQubeSide.SERVER, SonarEdition.COMMUNITY))
        val plugin = PlSqlPlugin()
        plugin.define(context)
        assertThat(context.extensions).hasSize(12)
    }

}
