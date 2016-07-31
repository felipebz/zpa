/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
package org.sonar.plsqlopen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.metadata.FormsMetadata;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {
    
    @Mock
    private SensorContext context;
    
    @Mock
    private PlSqlChecks checks;
    
    @Ignore
    @Test
    public void addIssue() throws Exception {
        PlSqlCheck expectedCheck = new CustomCheck();
        
        DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
        DefaultInputFile inputFile = new DefaultInputFile(".", "file.sql");
        inputFile.setLines(3);
        fileSystem.add(inputFile);

        when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck));
        when(this.checks.ruleKey(any(PlSqlCheck.class))).thenReturn(mock(RuleKey.class));
        
        SensorStorage storage = mock(SensorStorage.class);
        DefaultIssue newIssue = new DefaultIssue(storage);
        when(context.fileSystem()).thenReturn(fileSystem);
        when(context.newIssue()).thenReturn(newIssue);

        SonarComponents sonarComponents = new SonarComponents(context);
        sonarComponents.setChecks(checks);

        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on wrong line", -5), inputFile);
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 2), inputFile);
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 2), new DefaultInputFile(".", "."));
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 2), new DefaultInputFile(".", "unknown_file"));
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "other message", 3), inputFile);
        
        //verify(issuable, times(5)).addIssue(any(Issue.class));
        
        try {
            sonarComponents.reportIssue(inputFile, mock(RuleKey.class), mock(AnalyzerMessage.class));
            fail("NoClassDefFoundError expected");
        } catch (NoClassDefFoundError e) {
            assertThat(e.getMessage()).isEqualTo("org/sonar/api/batch/fs/InputComponent");
        }
    }
    
    @Test
    public void testInputFromIOFile() throws Exception {
        DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
        DefaultInputFile inputFile = new DefaultInputFile(".", "file.sql");
        fileSystem.add(inputFile);
        when(context.fileSystem()).thenReturn(fileSystem);
        
        SonarComponents sonarComponents = new SonarComponents(context);
        assertThat(sonarComponents.inputFromIOFile(new File("file.sql"))).isNotNull();
        assertThat(sonarComponents.inputFromIOFile(new File("unknown"))).isNull();
    }
    
    @Test
    public void canReadSimpleMetadaFile() {
        SonarComponents sonarComponents = new SonarComponents(context);
        sonarComponents.loadMetadataFile("src/test/resources/metadata/metadata.json");
        FormsMetadata metadata = sonarComponents.getFormsMetadata();
        
        assertThat(metadata.getAlerts()).containsExactly("foo", "bar");
        assertThat(metadata.getLovs()).containsExactly("foo", "bar");
    }
    
    private static class CustomCheck extends PlSqlCheck {

    }
    
}
