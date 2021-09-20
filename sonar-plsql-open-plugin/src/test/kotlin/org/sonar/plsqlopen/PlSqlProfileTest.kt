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
package org.sonar.plsqlopen

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context
import org.sonar.plsqlopen.checks.CheckList

class PlSqlProfileTest {

    @Test
    fun should_create_sonar_way_profile() {
        val context = Context()

        val definition = PlSqlProfile()
        definition.define(context)

        val profile = context.profile(PlSql.KEY, CheckList.SONAR_WAY_PROFILE)
        assertThat(profile).isNotNull

        val activeRules = profile.rules()
        assertThat(activeRules.size).isGreaterThan(40)
    }

}
