package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class IfWithExitCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("if_with_exit.sql", new IfWithExitCheck());
        final String message = "Replace this code by a EXIT WHEN statement.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage(message)
            .noMore();
    }

}
