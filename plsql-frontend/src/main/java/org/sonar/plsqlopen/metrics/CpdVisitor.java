package org.sonar.plsqlopen.metrics;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.plsqlopen.TokenLocation;
import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

public class CpdVisitor extends PlSqlVisitor {
    
    private NewCpdTokens newCpdTokens;
    
    public CpdVisitor(SensorContext context, InputFile inputFile) {
        newCpdTokens = context.newCpdTokens().onFile(inputFile);
    }
    
    @Override
    public void init() {
        subscribeTo(PlSqlGrammar.ANONYMOUS_BLOCK,
                    PlSqlGrammar.CREATE_PROCEDURE, 
                    PlSqlGrammar.CREATE_FUNCTION,
                    PlSqlGrammar.CREATE_PACKAGE_BODY);
    }
    
    @Override
    public void visitNode(AstNode node) {
        for (Token token : node.getTokens()) {
            saveCpdTokens(token);
        }
    }
    
    public void saveCpdTokens(Token token) {
        TokenLocation location = TokenLocation.from(token);
        newCpdTokens.addToken(location.line(), location.column(), location.endLine(), location.endColumn(), token.getValue());
    }
    
    @Override
    public void leaveFile(AstNode node) {
        newCpdTokens.save();
    }

}
