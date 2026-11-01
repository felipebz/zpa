//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.oracle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.jar.JarFile

/** The shaded plugin carries the acquisition code and never the Oracle JDBC driver. */
class ShadedPluginJarIT {
    @Test
    fun containsTheAcquisitionClassesAndNoJdbcDriver() {
        JarFile(System.getProperty("zpa.plugin.jar")).use { jar ->
            val names = jar.entries().asSequence().map { it.name }.toList()
            assertThat(names).contains(
                "com/felipebz/zpa/oracle/metadata/OracleMetadataAcquisition.class",
                "com/felipebz/zpa/oracle/metadata/OracleMetadataRequest.class",
                "com/felipebz/zpa/oracle/JdbcOracleMetadataAcquirer.class",
                "org/sonar/plugins/plsqlopen/api/project/ProjectExternalContext.class"
            )
            assertThat(names).noneMatch { it.startsWith("oracle/") || it.contains("ojdbc") }
            assertThat(names).doesNotContain("META-INF/services/java.sql.Driver")
            assertThat(names).noneMatch { it.startsWith("com/felipebz/zpa/api/") }
        }
    }
}
