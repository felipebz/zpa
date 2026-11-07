//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa

import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.sonar.api.Plugin
import org.sonar.api.SonarEdition
import org.sonar.api.SonarQubeSide
import org.sonar.api.config.PropertyDefinition
import org.sonar.api.internal.SonarRuntimeImpl
import org.sonar.api.utils.Version

class PlSqlPluginTest {

    @Test
    fun testGetExtensions() {
        val context = Plugin.Context(SonarRuntimeImpl.forSonarQube(Version.create(6, 0), SonarQubeSide.SERVER, SonarEdition.COMMUNITY))
        val plugin = PlSqlPlugin()
        plugin.define(context)
        assertThat(context.extensions).hasSize(15)
    }

    @Test
    fun oracleAnalysisParametersAreNotDeclaredAsSettings() {
        val context = Plugin.Context(SonarRuntimeImpl.forSonarQube(Version.create(25, 8), SonarQubeSide.SERVER, SonarEdition.COMMUNITY))
        PlSqlPlugin().define(context)
        // Declared properties are offered in the server's settings; connection details must come from the analysis only.
        assertThat(context.extensions.filterIsInstance<PropertyDefinition>().map { it.key() })
            .noneMatch { it.startsWith("sonar.zpa.oracle.") }
    }

}
