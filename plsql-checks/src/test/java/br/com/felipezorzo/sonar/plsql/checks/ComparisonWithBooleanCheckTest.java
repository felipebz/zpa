package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ComparisonWithBooleanCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("comparison_with_boolean.sql", new ComparisonWithBooleanCheck());
        final String messageWithTrue = "Remove the literal \"TRUE\" of this comparison.";
        final String messageWithFalse = "Remove the literal \"FALSE\" of this comparison.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(messageWithTrue)
            .next().atLine(3).withMessage(messageWithTrue)
            .next().atLine(4).withMessage(messageWithFalse)
            .next().atLine(5).withMessage(messageWithFalse)
            .next().atLine(6).withMessage(messageWithTrue)
            .noMore();
    }

}
