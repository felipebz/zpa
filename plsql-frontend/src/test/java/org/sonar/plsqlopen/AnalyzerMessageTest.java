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
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sonar.plsqlopen.AnalyzerMessage.TextSpan;
import org.sonar.plsqlopen.checks.PlSqlCheck;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public class AnalyzerMessageTest {
    
    @Test
    public void testAnalyzerMessage() {
        PlSqlCheck check = mock(PlSqlCheck.class);
        int line = 5;
        String message = "analyzer message";
        AnalyzerMessage analyzerMessage = new AnalyzerMessage(check, message, line);
        assertThat(analyzerMessage.getCheck()).isEqualTo(check);
        assertThat(analyzerMessage.getLine()).isEqualTo(line);
        assertThat(analyzerMessage.getDefaultMessage()).isEqualTo(message);

        AnalyzerMessage.TextSpan location = analyzerMessage.getLocation();
        assertThat(location.startLine).isEqualTo(line);
        assertThat(location.startCharacter).isEqualTo(-1);
        assertThat(location.endLine).isEqualTo(line);
        assertThat(location.endCharacter).isEqualTo(-1);
        assertThat(location.toString()).isEqualTo("(5:-1)-(5:-1)");
    }

    @Test
    public void testAnalyzerMessageOnFile2() {
        PlSqlCheck check = mock(PlSqlCheck.class);
        String message = "analyzer message";
        AnalyzerMessage analyzerMessage = new AnalyzerMessage(check, message, -5);
        assertThat(analyzerMessage.getCheck()).isEqualTo(check);
        assertThat(analyzerMessage.getLine()).isEqualTo(null);
        assertThat(analyzerMessage.getDefaultMessage()).isEqualTo(message);
        assertThat(analyzerMessage.getLocation()).isNull();
    }
    
    @Test
    public void testAnalyzerWithSecondaryLocation() {
        PlSqlCheck check = mock(PlSqlCheck.class);
        AnalyzerMessage secondary = mock(AnalyzerMessage.class);
        AnalyzerMessage analyzerMessage = new AnalyzerMessage(check, "analyzer message", 1);
        analyzerMessage.addSecondaryLocation(secondary);
        assertThat(analyzerMessage.getSecondaryLocations()).containsExactly(secondary);
    }
    
    @Test
    public void testCreateTextSpan() {
        int startLine = 1;
        int startCharacter = 1;
        int endLine = 2;
        int endCharacter = 2;
        
        Token token = mock(Token.class);
        when(token.getLine()).thenReturn(startLine);
        when(token.getColumn()).thenReturn(startCharacter);
        when(token.getOriginalValue()).thenReturn("");
        
        Token lastToken = mock(Token.class);
        when(lastToken.getLine()).thenReturn(endLine);
        when(lastToken.getColumn()).thenReturn(endCharacter);
        when(lastToken.getOriginalValue()).thenReturn("xxx");
        
        AstNode node = mock(AstNode.class);
        when(node.getToken()).thenReturn(token);
        when(node.getLastToken()).thenReturn(lastToken);
        
        TextSpan span = AnalyzerMessage.textSpanFor(node);
        assertThat(span.startLine).isEqualTo(startLine);
        assertThat(span.startCharacter).isEqualTo(startCharacter);
        assertThat(span.endLine).isEqualTo(endLine);
        assertThat(span.endCharacter).isEqualTo(endCharacter + 3);
    }
    
    
}
