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
package org.sonar.plsqlopen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

public class PlSqlIssueTest {
    
    private RuleKey ruleKey;
    private SensorContext sensorContext;
    private DefaultInputFile file;
    private SensorStorage storage;
    private DefaultIssue newIssue;
    private PlSqlIssue javaIssue;
    
    @Before
    public void setup() {
        file = new TestInputFileBuilder("module", "relPath")
                .setLines(3)
                .setOriginalLineOffsets(new int[] { 0, 10, 15 })
                .setLastValidOffset(25).build();
        ruleKey = RuleKey.of("squid", "ruleKey");
        sensorContext = mock(SensorContext.class);
        storage = mock(SensorStorage.class);
        newIssue = new DefaultIssue(storage);
        when(sensorContext.newIssue()).thenReturn(newIssue);
        javaIssue = PlSqlIssue.create(sensorContext, ruleKey, null);
    }

    @Test
    public void issueWithSecondaryLocations() {
        javaIssue.setPrimaryLocation(file, "main message", 1, 2, 1, 6);
        javaIssue.addSecondaryLocation(file, 2, 2, 2, 4, "secondary message 1");
        javaIssue.addSecondaryLocation(file, 3, 1, 3, 5, "secondary message 2");
        javaIssue.save();

        verify(storage, times(1)).store(newIssue);

        assertThat(newIssue.ruleKey()).isEqualTo(ruleKey);
        assertLocation(newIssue.primaryLocation(), file, "main message", 1, 2, 1, 6);
        assertThat(newIssue.flows()).hasSize(2);
        assertLocation(newIssue.flows().get(0).locations().get(0), file, "secondary message 1", 2, 2, 2, 4);
        assertLocation(newIssue.flows().get(1).locations().get(0), file, "secondary message 2", 3, 1, 3, 5);
    }
    
    @Test
    public void issueOnFile() {
        javaIssue = PlSqlIssue.create(sensorContext, ruleKey, null);
        javaIssue.setPrimaryLocationOnFile(file, "file message");
        javaIssue.save();

        verify(storage, times(1)).store(newIssue);
        assertThat(newIssue.ruleKey()).isEqualTo(ruleKey);
        IssueLocation location = newIssue.primaryLocation();
        assertThat(location.inputComponent()).isEqualTo(file);
        assertThat(location.textRange()).isNull();
        assertThat(location.message()).isEqualTo("file message");
    }
    
    @Test
    public void issueOnEntireLine() {
        javaIssue = PlSqlIssue.create(sensorContext, ruleKey, null);
        javaIssue.setPrimaryLocation(file, "line message", 2, -1, 2, -1);
        javaIssue.save();

        verify(storage, times(1)).store(newIssue);
        assertLocation(newIssue.primaryLocation(), file, "line message", 2, 0, 2, 4);
    }

    private static void assertLocation(IssueLocation location, InputFile file, String message, int startLine,
            int startOffset, int endLine, int endOffset) {
        assertThat(location.inputComponent()).isEqualTo(file);
        assertThat(location.message()).isEqualTo(message);
        TextRange textRange = location.textRange();
        TextPointer start = textRange.start();
        assertThat(start.line()).isEqualTo(startLine);
        assertThat(start.lineOffset()).isEqualTo(startOffset);
        TextPointer end = textRange.end();
        assertThat(end.line()).isEqualTo(endLine);
        assertThat(end.lineOffset()).isEqualTo(endOffset);
    }

}
