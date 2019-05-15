/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.Symbol;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

public class SonarQubeSymbolTableTest {

    @Test
    public void test() throws URISyntaxException {
        DefaultInputFile inputFile = new TestInputFileBuilder("key", new File("test.sql").getPath())
            .setContents("abcde")
            .setLanguage("plsqlopen")
            .setCharset(StandardCharsets.UTF_8)
            .build();
        String key = inputFile.key();

        SensorContextTester context = SensorContextTester.create(new File(""));
        context.fileSystem().add(inputFile);

        Symbol symbol = createSymbol(createNode(1));
        symbol.addUsage(createNode(2));
        symbol.addUsage(createNode(3));

        SonarQubeSymbolTable symbolTable = new SonarQubeSymbolTable(context, inputFile);
        symbolTable.save(Collections.singletonList(symbol));

        assertThat(context.referencesForSymbolAt(key, 1, 1))
            .extracting("start.line", "start.lineOffset")
            .containsExactly(tuple(1, 2), tuple(1, 3));
    }

    private AstNode createNode(int character) throws URISyntaxException {
        Token token = Token.builder()
            .setLine(1)
            .setColumn(character)
            .setValueAndOriginalValue(" ")
            .setType(GenericTokenType.IDENTIFIER)
            .setURI(new URI("tests://unittest"))
            .build();

        Token lastToken = Token.builder()
            .setLine(1)
            .setColumn(character + 2)
            .setValueAndOriginalValue(" ")
            .setType(GenericTokenType.IDENTIFIER)
            .setURI(new URI("tests://unittest"))
            .build();

        AstNode node = new AstNode(token);
        node.addChild(new AstNode(token));
        node.addChild(new AstNode(lastToken));

        return node;
    }

    private Symbol createSymbol(AstNode node) {
        return new Symbol(node, Symbol.Kind.VARIABLE, mock(Scope.class), null);
    }

}
