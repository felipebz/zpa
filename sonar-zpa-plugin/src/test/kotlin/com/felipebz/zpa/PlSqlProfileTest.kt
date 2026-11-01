//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import com.felipebz.zpa.checks.CheckList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.Context

class PlSqlProfileTest {

    @Test
    fun should_create_sonar_way_profile() {
        val context = Context()

        val definition = PlSqlProfile()
        definition.define(context)

        val profile = context.profile(PlSql.KEY, CheckList.SONAR_WAY_PROFILE)
        assertThat(profile).isNotNull

        val activeRules = profile.rules()
        assertThat(activeRules.size).isGreaterThan(50)
    }

}
