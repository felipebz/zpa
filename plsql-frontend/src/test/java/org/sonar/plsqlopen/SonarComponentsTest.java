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
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.squidbridge.SquidAstVisitor;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Grammar;

@RunWith(MockitoJUnitRunner.class)
public class SonarComponentsTest {
    
    @Mock
    private ResourcePerspectives resourcePerspectives;

    @Mock
    private SensorContext context;
    
    @Mock
    private PlSqlChecks checks;
    
    @Test
    public void test() {
        DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
        Issuable issuable = mock(Issuable.class);
        when(resourcePerspectives.as(eq(Issuable.class), any(InputFile.class))).thenReturn(issuable);

        SonarComponents sonarComponents = new SonarComponents(resourcePerspectives, context, fileSystem);

        assertThat(sonarComponents.issuableFor(mock(InputFile.class))).isEqualTo(issuable);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void addIssue() throws Exception {
        SquidAstVisitor<Grammar> expectedCheck = new CustomCheck();
        
        DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
        InputFile inputFile = new DefaultInputFile("file.sql");
        fileSystem.add(inputFile);

        Issuable issuable = mock(Issuable.class);
        Issuable.IssueBuilder issueBuilder = mock(Issuable.IssueBuilder.class);
        when(issuable.newIssueBuilder()).thenReturn(issueBuilder);
        when(issueBuilder.ruleKey(any(RuleKey.class))).thenReturn(issueBuilder);
        when(issueBuilder.message(anyString())).thenReturn(issueBuilder);
        when(issueBuilder.line(anyInt())).thenReturn(issueBuilder);
        when(issueBuilder.effortToFix(anyDouble())).thenReturn(issueBuilder);
        when(resourcePerspectives.as(eq(Issuable.class), any(InputFile.class))).thenReturn(issuable);
        when(this.checks.all()).thenReturn(Lists.newArrayList(expectedCheck));
        when(this.checks.ruleKey(any(SquidAstVisitor.class))).thenReturn(mock(RuleKey.class));

        SonarComponents sonarComponents = new SonarComponents(resourcePerspectives, context, fileSystem);
        sonarComponents.setChecks(checks);

        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on wrong line", -5), inputFile);
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 42), inputFile);
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 42), new DefaultInputFile("."));
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "message on line", 42), new DefaultInputFile("unknown_file"));
        sonarComponents.reportIssue(new AnalyzerMessage(expectedCheck, "other message", 35), inputFile);
        
        verify(issuable, times(5)).addIssue(any(Issue.class));
        
        try {
            sonarComponents.reportIssueAfterSQ52(inputFile, mock(RuleKey.class), mock(AnalyzerMessage.class));
            fail("NoClassDefFoundError expected");
        } catch (NoClassDefFoundError e) {
            assertThat(e.getMessage()).isEqualTo("org/sonar/api/batch/fs/InputComponent");
        }
    }
    
    @Test
    public void testInputFromIOFile() throws Exception {
        DefaultFileSystem fileSystem = new DefaultFileSystem(new File(""));
        InputFile inputFile = new DefaultInputFile("file.sql");
        fileSystem.add(inputFile);
        
        SonarComponents sonarComponents = new SonarComponents(resourcePerspectives, context, fileSystem);
        assertThat(sonarComponents.inputFromIOFile(new File("file.sql"))).isNotNull();
        assertThat(sonarComponents.inputFromIOFile(new File("unknown"))).isNull();
    }
    
    private static class CustomCheck extends SquidAstVisitor<Grammar> {

    }
    
}
