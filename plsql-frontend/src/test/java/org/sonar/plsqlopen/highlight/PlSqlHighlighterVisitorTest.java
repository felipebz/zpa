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
package org.sonar.plsqlopen.highlight;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public class PlSqlHighlighterVisitorTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    private String eol;

    @Test
    public void shouldAnalyse_lf() throws IOException {
    	eol = "\n";
    	verifyHighlighting();
    }
    
    @Test
    public void shouldAnalyse_crlf() throws IOException {
    	eol = "\r\n";
    	verifyHighlighting();
    }
    
    @Test
    public void shouldAnalyse_cr() throws IOException {
    	eol = "\r";
    	verifyHighlighting();
    }
    
    private void verifyHighlighting() throws IOException {
    	File baseDir = temp.newFolder();
        File file = new File(baseDir, "test.sql");
        String content = Files.toString(new File("src/test/resources/highlight/highlight.sql"), StandardCharsets.UTF_8);
        Files.write(content.replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, StandardCharsets.UTF_8);
        
        DefaultInputFile inputFile = new TestInputFileBuilder("key", "test.sql")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(Files.toString(file, StandardCharsets.UTF_8))
                .setModuleBaseDir(baseDir.toPath())
                .build();
        
        SensorContextTester context = SensorContextTester.create(baseDir);
        context.fileSystem().add(inputFile);
        
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, ImmutableList.of(), new NoSonarFilter(), null, false);
        scanner.scanFile(inputFile);
        
        String key = inputFile.key();
        assertThat(context.highlightingTypeAt(key, 1, lineOffset(1))).containsExactly(TypeOfText.KEYWORD);
        assertThat(context.highlightingTypeAt(key, 2, lineOffset(3))).containsExactly(TypeOfText.COMMENT);
        assertThat(context.highlightingTypeAt(key, 3, lineOffset(3))).containsExactly(TypeOfText.STRUCTURED_COMMENT);
        assertThat(context.highlightingTypeAt(key, 6, lineOffset(8))).containsExactly(TypeOfText.STRING);
        assertThat(context.highlightingTypeAt(key, 7, lineOffset(1))).containsExactly(TypeOfText.KEYWORD);
    }
    
    private int lineOffset(int offset) {
        return offset - 1;
    }
}
