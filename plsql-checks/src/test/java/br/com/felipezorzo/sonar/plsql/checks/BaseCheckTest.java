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
package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;
import java.util.Locale;

import org.junit.Before;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;

import com.sonar.sslr.api.Grammar;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class BaseCheckTest {

    private final String defaultResourceFolder = "src/test/resources/checks/";
    
    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }
    
    protected SourceFile scanSingleFile(String filename, SquidAstVisitor<Grammar> check) {
        return PlSqlAstScanner.scanSingleFile(new File(defaultResourceFolder + filename), check);
    }
    
}
