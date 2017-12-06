package org.sonar.plsqlopen.metrics;

import java.util.HashSet;
import java.util.Set;

import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.squid.PlSqlCommentAnalyzer;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;

public class MetricsVisitor extends PlSqlVisitor {
    
    private static final PlSqlCommentAnalyzer COMMENT_ANALYSER = new PlSqlCommentAnalyzer();
    
    private int numberOfStatements;
    private Set<Integer> linesOfCode = new HashSet<>();
    private Set<Integer> linesOfComments = new HashSet<>();
    private Set<Integer> noSonar = new HashSet<>();
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.STATEMENT);
    }
    
    @Override
    public void visitNode(AstNode node) {
        if (node.is(PlSqlGrammar.STATEMENT)) {
            numberOfStatements++;
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

}
