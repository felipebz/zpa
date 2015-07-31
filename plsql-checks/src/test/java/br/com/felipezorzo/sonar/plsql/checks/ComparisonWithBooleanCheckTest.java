package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class ComparisonWithBooleanCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        ComparisonWithBooleanCheck check = new ComparisonWithBooleanCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/comparison_with_boolean.sql"), check);
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
