/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plsqlopen.highlight;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Highlightable;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class PlSqlHighlighterTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
  
    private PlSqlHighlighter highlighter;
    private ResourcePerspectives perspectives;
    private List<String> lines;
    private String eol;
    private Highlightable highlightable;
    private Highlightable.HighlightingBuilder highlightingBuilder;

    @Before
    public void setUp() {
        perspectives = mock(ResourcePerspectives.class);
        highlighter = new PlSqlHighlighter(new PlSqlConfiguration(Charsets.UTF_8));
        highlightable = mock(Highlightable.class);
        highlightingBuilder = mock(Highlightable.HighlightingBuilder.class);
        when(perspectives.as(Mockito.eq(Highlightable.class), Mockito.any(InputFile.class))).thenReturn(highlightable);
        when(highlightable.newHighlighting()).thenReturn(highlightingBuilder);
    }
    
    private File generateTestFile() throws IOException {
        File file = temp.newFile();
        String content = Files.toString(new File("src/test/resources/br/com/felipezorzo/sonar/plsql/highlight.sql"), Charsets.UTF_8);
        Files.write(content.replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, Charsets.UTF_8);
        return file;
    }
    
    private int offset(int line, int column) {
    	int result = 0;
    	for (int i = 0; i < line - 1; i++) {
    		result += lines.get(i).length() + eol.length();
    	}
    	result += column - 1;
    	return result;
    }

    @Test
    public void shouldAnalyse_lf() throws IOException {
    	eol = "\n";
    	File file = generateTestFile();
    	verifyHighlighting(file);
    }
    
    @Test
    public void shouldAnalyse_crlf() throws IOException {
    	eol = "\r\n";
    	File file = generateTestFile();
    	verifyHighlighting(file);
    }
    
    @Test
    public void shouldAnalyse_cr() throws IOException {
    	eol = "\r";
    	File file = generateTestFile();
    	verifyHighlighting(file);
    }
    
    private void verifyHighlighting(File file) throws IOException {
    	lines = Files.readLines(file, Charsets.UTF_8);

        highlighter.highlight(highlightable, file);

        verify(highlightingBuilder).highlight(offset(1, 1), offset(1, 6), "k");
        verify(highlightingBuilder).highlight(offset(2, 3), offset(2, 23), "cd");
        verify(highlightingBuilder).highlight(offset(3, 3), offset(5, 6), "j");
        verify(highlightingBuilder).highlight(offset(6, 8), offset(6, 9), "s");
        verify(highlightingBuilder).highlight(offset(7, 1), offset(7, 4), "k");
        verify(highlightingBuilder).done();
        verifyNoMoreInteractions(highlightingBuilder);
    }
}
