package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class CollapsibleIfStatementsCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("collapsible_if_statements.sql", new CollapsibleIfStatementsCheck());
        String message = "Merge this if statement with the enclosing one.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(4).withMessage(message)
            .next().atLine(10).withMessage(message)
            .next().atLine(11).withMessage(message)
            .noMore();
    }
    
}
