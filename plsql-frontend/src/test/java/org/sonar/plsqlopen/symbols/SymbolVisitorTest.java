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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class SymbolVisitorTest {
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
  
    private SensorContextTester context;
    private String eol;
    private File baseDir;
    
    private void scanFile() throws IOException {
        baseDir = temp.newFolder();
        File file = new File(baseDir, "test.sql");
        String content = Files.toString(new File("src/test/resources/symbols/symbols.sql"), Charsets.UTF_8);
        Files.write(content.replaceAll("\\r\\n", "\n").replaceAll("\\n", eol), file, Charsets.UTF_8);
        
        DefaultInputFile inputFile = new DefaultInputFile("key", "test.sql").setLanguage("plsqlopen")
                .initMetadata(Files.toString(file, Charsets.UTF_8));
        
        context = SensorContextTester.create(baseDir);
        context.fileSystem().add(inputFile);
        
        ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
        
        SonarComponents components = new SonarComponents(resourcePerspectives, context, context.fileSystem()).getTestInstance();
        
        SymbolVisitor symbolVisitor = new SymbolVisitor();
        
        PlSqlAstScanner.scanSingleFile(inputFile.file(), components, symbolVisitor);
    }
    
    @Test
    public void shouldAnalyse_lf() throws IOException {
        eol = "\n";
        verifySymbols();
    }
    
    @Test
    public void shouldAnalyse_crlf() throws IOException {
        eol = "\r\n";
        verifySymbols();
    }
    
    @Test
    public void shouldAnalyse_cr() throws IOException {
        eol = "\r";
        verifySymbols();
    }
    
    private void verifySymbols() throws IOException {
        scanFile();
        
        String key = "key:test.sql";
        
        assertThat(context.referencesForSymbolAt(key, 2, lineOffset(3)))
            .extracting("start.line", "start.lineOffset")
             .containsExactly(tuple(4, lineOffset(3)));

        assertThat(context.referencesForSymbolAt(key, 7, lineOffset(7))).isEmpty();
        assertThat(context.referencesForSymbolAt(key, 11, lineOffset(12))).isEmpty();
        
        assertThat(context.referencesForSymbolAt(key, 12, lineOffset(10)))
            .extracting("start.line", "start.lineOffset")
            .containsExactly(tuple(15, lineOffset(8)));
        
        assertThat(context.referencesForSymbolAt(key, 12, lineOffset(14))).isEmpty();
        assertThat(context.referencesForSymbolAt(key, 18, lineOffset(21))).isEmpty();
        assertThat(context.referencesForSymbolAt(key, 24, lineOffset(3))).isEmpty();
        assertThat(context.referencesForSymbolAt(key, 28, lineOffset(3))).isEmpty();
        
        assertThat(context.referencesForSymbolAt(key, 32, lineOffset(3)))
            .extracting("start.line", "start.lineOffset")
            .containsExactly(tuple(36, lineOffset(8)));
    }
    
    private int lineOffset(int offset) {
        return offset - 1;
    }
    
}
