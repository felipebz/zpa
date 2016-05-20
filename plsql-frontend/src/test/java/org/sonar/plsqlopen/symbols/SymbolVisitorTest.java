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
package org.sonar.plsqlopen.symbols;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class SymbolVisitorTest {
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
  
    private List<String> lines;
    private String eol;
    private Symbolizable symbolizable = mock(Symbolizable.class);
    private Symbolizable.SymbolTableBuilder symboltableBuilder = mock(Symbolizable.SymbolTableBuilder.class);
    
    private void scanFile(String filename) {
        String relativePath = filename;
        DefaultInputFile inputFile = new DefaultInputFile(".", relativePath).setLanguage("plsqlopen");
        DefaultFileSystem fs = new DefaultFileSystem(new File("."));
        fs.add(inputFile);
        
        when(symbolizable.newSymbolTableBuilder()).thenReturn(symboltableBuilder);
        
        SensorContext context = mock(SensorContext.class);
        ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
        when(resourcePerspectives.as(Mockito.eq(Symbolizable.class), Mockito.any(InputFile.class))).thenReturn(symbolizable);
        
        SonarComponents components = new SonarComponents(resourcePerspectives, context, fs).getTestInstance();
        
        SymbolVisitor symbolVisitor = new SymbolVisitor();
        
        PlSqlAstScanner.scanSingleFile(new File(filename), components, symbolVisitor);
    }
    
    private File generateTestFile() throws IOException {
        File file = temp.newFile();
        String content = Files.toString(new File("src/test/resources/symbols/symbols.sql"), Charsets.UTF_8);
        Files.write(content.replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, Charsets.UTF_8);
        return file;
    }
    
    @Test
    public void shouldAnalyse_lf() throws IOException {
        eol = "\n";
        File file = generateTestFile();
        verifySymbols(file);
    }
    
    @Test
    public void shouldAnalyse_crlf() throws IOException {
        eol = "\r\n";
        File file = generateTestFile();
        verifySymbols(file);
    }
    
    @Test
    public void shouldAnalyse_cr() throws IOException {
        eol = "\r";
        File file = generateTestFile();
        verifySymbols(file);
    }
    
    private void verifySymbols(File file) throws IOException {
        lines = Files.readLines(file, Charsets.UTF_8);
        
        scanFile(file.getAbsolutePath());
        
        verify(symboltableBuilder).newSymbol(offset(2, 3), offset(2, 4));
        verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(4, 3)));
        verify(symboltableBuilder).newSymbol(offset(6, 7), offset(6, 8));
        verify(symboltableBuilder).newSymbol(offset(11, 22), offset(11, 23));
        verify(symboltableBuilder).newSymbol(offset(12, 10), offset(12, 13));
        verify(symboltableBuilder).newSymbol(offset(12, 14), offset(12, 15));
        verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(15, 8)));
        verify(symboltableBuilder).newSymbol(offset(18, 21), offset(18, 22));
        verify(symboltableBuilder).newSymbol(offset(24, 3), offset(24, 6));
        verify(symboltableBuilder).newSymbol(offset(28, 3), offset(28, 6));
        verify(symboltableBuilder).newSymbol(offset(32, 3), offset(32, 5));
        verify(symboltableBuilder).newReference(any(Symbol.class), eq(offset(36, 8)));
        
        verify(symboltableBuilder).build();
        verifyNoMoreInteractions(symboltableBuilder);
    }
    
    private int offset(int line, int column) {
        int result = 0;
        for (int i = 0; i < line - 1; i++) {
            result += lines.get(i).length() + eol.length();
        }
        result += column - 1;
        return result;
    }
    
}
