package br.com.felipezorzo.sonar.plsql;

import static com.sonar.sslr.api.GenericTokenType.EOF;

import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.measures.MetricDef;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;

public class PlSqlLinesOfCodeVisitor<T> extends SquidAstVisitor<Grammar> implements AstAndTokenVisitor {

    private final MetricDef metric;
    private int lastTokenLine;

    public PlSqlLinesOfCodeVisitor(MetricDef metric) {
        this.metric = metric;
    }

    @Override
    public void visitFile(AstNode node) {
        lastTokenLine = -1;
    }

    @Override
    public void visitToken(Token token) {
        if (!token.getType().equals(EOF)) {
            /* Handle all the lines of the token */
            String[] tokenLines = token.getValue().split("\n", -1);

            int firstLineAlreadyCounted = lastTokenLine == token.getLine() ? 1 : 0;
            getContext().peekSourceCode().add(metric, (double) tokenLines.length - firstLineAlreadyCounted);

            lastTokenLine = token.getLine() + tokenLines.length - 1;
        }
    }

}
