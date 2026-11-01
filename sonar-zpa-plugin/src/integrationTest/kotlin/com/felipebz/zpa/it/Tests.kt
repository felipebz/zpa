//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.it

import com.sonar.orchestrator.Orchestrator
import com.sonar.orchestrator.build.SonarScanner
import com.sonar.orchestrator.junit5.OrchestratorExtension
import com.sonar.orchestrator.locator.FileLocation
import org.sonarqube.ws.client.HttpConnector
import org.sonarqube.ws.client.WsClient
import org.sonarqube.ws.client.WsClientFactories
import java.io.File

object Tests {

    @JvmField
    val ORCHESTRATOR: OrchestratorExtension =
        OrchestratorExtension.builderEnv()
            .useDefaultAdminCredentialsForBuilds(true)
            .apply {
                val zipFile = System.getProperty("sonar.zipFile")
                if (zipFile != null) {
                    setZipFile(File(zipFile))
                } else {
                    setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[26.8]"))
                }
            }
            .setOrchestratorProperty("orchestrator.artifactory.url", "https://repo1.maven.org/maven2")
            .addPlugin(FileLocation.byWildcardMavenFilename(
                    File("build/libs"),
                    "sonar-zpa-plugin-*.jar"))
            .addPlugin(FileLocation.byWildcardMavenFilename(
                    File("../plsql-custom-rules/build/libs"),
                    "plsql-custom-rules-*.jar"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/com/felipebz/zpa/it/it-profile.xml"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/com/felipebz/zpa/it/empty-profile.xml"))
            .build()

    fun createSonarScanner(): SonarScanner =
        SonarScanner.create()
            .setScannerVersion("8.1.0.6389");

    fun newWsClient(orchestrator: Orchestrator): WsClient =
        WsClientFactories.getDefault()
                .newClient(HttpConnector.newBuilder()
                        .url(orchestrator.server.url).build())

}
