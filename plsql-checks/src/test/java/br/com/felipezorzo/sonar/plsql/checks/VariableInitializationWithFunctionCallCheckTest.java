package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class VariableInitializationWithFunctionCallCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("variable_initialization_with_function_call.sql", new VariableInitializationWithFunctionCallCheck());
        final String message = "Move this initialization to the BEGIN...END block.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .next().atLine(3).withMessage(message)
            .noMore();
    }
    
}
