/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
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
package org.sonar.plsqlopen.it

import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.build.SonarScanner
import com.sonar.orchestrator.junit5.OrchestratorExtension
import com.sonar.orchestrator.locator.FileLocation
import org.junit.jupiter.api.extension.RegisterExtension
import org.sonarqube.ws.client.HttpConnector
import org.sonarqube.ws.client.WsClient
import org.sonarqube.ws.client.WsClientFactories
import java.io.File

object Tests {

    @JvmField
    @RegisterExtension
    val ORCHESTRATOR: OrchestratorExtension =
        OrchestratorExtension.builderEnv()
            .useDefaultAdminCredentialsForBuilds(true)
            .apply {
                val zipFile = System.getProperty("sonar.zipFile")
                if (zipFile != null) {
                    setZipFile(File(zipFile))
                } else {
                    setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[9.9]"))
                }
            }
            .setOrchestratorProperty("orchestrator.artifactory.url", "https://repo1.maven.org/maven2")
            .addPlugin(FileLocation.byWildcardMavenFilename(
                    File("build/libs"),
                    "sonar-zpa-plugin-*.jar"))
            .addPlugin(FileLocation.byWildcardMavenFilename(
                    File("../plsql-custom-rules/build/libs"),
                    "plsql-custom-rules-*.jar"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/org/sonar/plsqlopen/it/it-profile.xml"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/org/sonar/plsqlopen/it/empty-profile.xml"))
            .build()

    fun createSonarScanner(): SonarScanner =
        SonarScanner.create()
            .setScannerVersion("6.2.1.4610")

    fun newWsClient(orchestrator: Orchestrator): WsClient =
        WsClientFactories.getDefault()
                .newClient(HttpConnector.newBuilder()
                        .url(orchestrator.server.url).build())

}
