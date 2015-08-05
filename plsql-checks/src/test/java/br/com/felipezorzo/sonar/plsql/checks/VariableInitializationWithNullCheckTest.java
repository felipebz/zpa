package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class VariableInitializationWithNullCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("variable_initialization_with_null.sql", new VariableInitializationWithNullCheck());
        final String message = "Remove this unnecessary initialization to NULL.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .next().atLine(3).withMessage(message)
            .next().atLine(4).withMessage(message)
            .next().atLine(5).withMessage(message)
            .next().atLine(7).withMessage(message)
            .noMore();
    }

}
