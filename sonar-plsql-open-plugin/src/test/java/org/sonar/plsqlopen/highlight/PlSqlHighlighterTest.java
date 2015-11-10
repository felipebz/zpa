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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Highlightable;
import org.sonar.plsqlopen.PlSql;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;

public class PlSqlHighlighterTest {

    private PlSqlHighlighter highlighter;
    private DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    ResourcePerspectives perspectives;

    @Before
    public void setUp() {
        perspectives = mock(ResourcePerspectives.class);
        highlighter = new PlSqlHighlighter(new PlSqlConfiguration(fs.encoding()));
    }

    @Test
    public void shouldAnalyse() {
        String relativePath = "src/test/resources/br/com/felipezorzo/sonar/plsql/highlight.sql";
        DefaultInputFile inputFile = new DefaultInputFile(relativePath).setLanguage(PlSql.KEY);
        inputFile.setAbsolutePath((new File(relativePath)).getAbsolutePath());
        fs.add(inputFile);

        Highlightable highlightable = mock(Highlightable.class);
        Highlightable.HighlightingBuilder highlightingBuilder = mock(Highlightable.HighlightingBuilder.class);
        when(perspectives.as(Mockito.eq(Highlightable.class), Mockito.any(InputFile.class))).thenReturn(highlightable);
        when(highlightable.newHighlighting()).thenReturn(highlightingBuilder);

        highlighter.highlight(highlightable, inputFile.file());

        verify(highlightingBuilder).highlight(0, 5, "k");
        verify(highlightingBuilder).highlight(8, 28, "cd");
        verify(highlightingBuilder).highlight(31, 61, "j");
        verify(highlightingBuilder).highlight(69, 70, "s");
        verify(highlightingBuilder).highlight(72, 75, "k");
        verify(highlightingBuilder).done();
        verifyNoMoreInteractions(highlightingBuilder);
    }
}
