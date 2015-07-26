package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class CollapsibleIfStatementsCheckTest {

    @Test
    public void matchesCollapsibleIf() {
        CollapsibleIfStatementsCheck check = new CollapsibleIfStatementsCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(
                new File("src/test/resources/checks/collapsible_if_statements_test.sql"), check);
        String message = "Merge this if statement with the enclosing one.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(4).withMessage(message)
            .next().atLine(10).withMessage(message)
            .next().atLine(11).withMessage(message)
            .noMore();
    }
    
}
