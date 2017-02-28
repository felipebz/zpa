/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
package org.sonar.plsqlopen.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;

import java.io.File;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

@RunWith(Suite.class)
@Suite.SuiteClasses({ MetricsTest.class, IssueTest.class, })
public class Tests {

    @ClassRule
    public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
            .addPlugin(FileLocation.byWildcardMavenFilename(
                    new File("../../sonar-plsql-open-plugin/target"),
                    "sonar-plsql-open-plugin-*.jar"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/org/sonar/plsqlopen/it/it-profile.xml"))
            .restoreProfileAtStartup(FileLocation.ofClasspath("/org/sonar/plsqlopen/it/empty-profile.xml")).build();

    public static SonarScanner createSonarScanner() {
        SonarScanner build = SonarScanner.create();
        return build;
    }
    
    public static WsClient newWsClient(Orchestrator orchestrator) {
        return WsClientFactories.getDefault()
                .newClient(HttpConnector.newBuilder()
                        .url(orchestrator.getServer().getUrl()).build());
    }

}
