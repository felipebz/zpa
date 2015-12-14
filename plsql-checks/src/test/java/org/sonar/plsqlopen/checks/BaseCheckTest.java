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
package org.sonar.plsqlopen.checks;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Collection;
import java.util.Locale;

import org.junit.Before;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.squidbridge.SquidAstVisitor;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.Grammar;

public class BaseCheckTest {

    private final String defaultResourceFolder = "src/test/resources/checks/";
    private DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    
    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }
    
    protected Collection<AnalyzerMessage> scanFile(String filename, SquidAstVisitor<Grammar> check) {
        String relativePath = defaultResourceFolder + filename;
        DefaultInputFile inputFile = new DefaultInputFile(relativePath).setLanguage("plsqlopen");
        inputFile.setAbsolutePath((new File(relativePath)).getAbsolutePath());
        fs.add(inputFile);
        
        SensorContext context = mock(SensorContext.class);
        ResourcePerspectives resourcePerspectives = mock(ResourcePerspectives.class);
        
        SonarComponents components = new SonarComponents(resourcePerspectives, context, fs).getTestInstance();
        
        PlSqlAstScanner.scanSingleFile(new File(defaultResourceFolder + filename), components, 
                ImmutableList.of(new SymbolVisitor(), check));
        return ((SonarComponents.Test) components).getIssues();
    }
    
}
