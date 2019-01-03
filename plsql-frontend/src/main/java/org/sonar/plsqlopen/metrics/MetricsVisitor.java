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
package org.sonar.plsqlopen.metrics;

import java.util.HashSet;
import java.util.Set;

import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.squid.PlSqlCommentAnalyzer;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

public class MetricsVisitor extends PlSqlCheck {
    
    private static final PlSqlCommentAnalyzer COMMENT_ANALYSER = new PlSqlCommentAnalyzer();
    
    private int numberOfStatements;
    private Set<Integer> linesOfCode = new HashSet<>();
    private Set<Integer> linesOfComments = new HashSet<>();
    private Set<Integer> noSonar = new HashSet<>();
    private Set<Integer> executableLines = new HashSet<>();
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (node.is(PlSqlGrammar.STATEMENT)) {
            numberOfStatements++;
            executableLines.add(node.getTokenLine());
        }
    }
    
    @Override
    public void visitToken(Token token) {
        String[] tokenLines = token.getValue().split("\n", -1);
        for (int line = token.getLine(); line < token.getLine() + tokenLines.length; line++) {
          linesOfCode.add(line);
        }

        for (Trivia trivia : token.getTrivia()) {
            if (trivia.isComment()) {
                visitComment(trivia);
            }
        }
    }
    
    public void visitComment(Trivia trivia) {
        String[] commentLines = COMMENT_ANALYSER.getContents(trivia.getToken().getOriginalValue()).split("(\r)?\n|\r",
                -1);
        int line = trivia.getToken().getLine();

        for (String commentLine : commentLines) {
            if (commentLine.contains("NOSONAR")) {
                linesOfComments.remove(line);
                noSonar.add(line);
            } else if (!COMMENT_ANALYSER.isBlank(commentLine)) {
                linesOfComments.add(line);
            }
            line++;
        }
    }

    public int getNumberOfStatements() {
        return numberOfStatements;
    }
    
    public Set<Integer> getLinesOfCode() {
        return linesOfCode;
    }
    
    public Set<Integer> getLinesOfComments() {
        return linesOfComments;
    }

    public Set<Integer> getLinesWithNoSonar() {
        return noSonar;
    }
    
    public Set<Integer> getExecutableLines() {
        return executableLines;
    }

}
