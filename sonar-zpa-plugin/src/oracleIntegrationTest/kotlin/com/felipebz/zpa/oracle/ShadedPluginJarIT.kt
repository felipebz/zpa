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
