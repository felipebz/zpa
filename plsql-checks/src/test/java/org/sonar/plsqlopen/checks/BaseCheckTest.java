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
package org.sonar.plsqlopen.checks;

import java.io.File;
import java.util.Collection;
import java.util.Locale;

import org.junit.Before;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.squidbridge.SquidAstVisitor;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.Grammar;

public class BaseCheckTest {

    private static final String defaultResourceFolder = "src/test/resources/checks/";
    private DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    
    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }
    
    protected String getPath(String filename) {
        return defaultResourceFolder + filename;
    }
    
    protected Collection<AnalyzerMessage> scanFile(String filename, SquidAstVisitor<Grammar> check) {
        String relativePath = defaultResourceFolder + filename;
        DefaultInputFile inputFile = new DefaultInputFile("key", relativePath).setLanguage("plsqlopen");
        fs.add(inputFile);
        
        SensorContextTester context = SensorContextTester.create(new File("."));
        context.setFileSystem(fs);
        
        SonarComponents components = new SonarComponents(context).getTestInstance();
        
        PlSqlAstScanner.scanSingleFile(new File(relativePath), components, 
                ImmutableList.of(new SymbolVisitor(), check));
        return ((SonarComponents.Test) components).getIssues();
    }
    
}
