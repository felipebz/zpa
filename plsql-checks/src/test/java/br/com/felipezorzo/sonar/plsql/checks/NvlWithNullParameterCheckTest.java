package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class NvlWithNullParameterCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("nvl_with_null_parameter.sql", new NvlWithNullParameterCheck());
        final String messageWithNull = "Remove the NULL parameter of this NVL.";
        final String messageWithEmptyString = "Remove the '' parameter of this NVL.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(messageWithNull)
            .next().atLine(3).withMessage(messageWithNull)
            .next().atLine(4).withMessage(messageWithEmptyString)
            .next().atLine(5).withMessage(messageWithEmptyString)
            .noMore();
    }
    
}
