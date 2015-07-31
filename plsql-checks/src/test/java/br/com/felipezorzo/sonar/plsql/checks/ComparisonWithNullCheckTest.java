package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class ComparisonWithNullCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        ComparisonWithNullCheck check = new ComparisonWithNullCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/comparison_with_null.sql"), check);
        final String messageWithIsNull = "Fix this comparison or change to \"IS NULL\".";
        final String messageWithIsNotNull = "Fix this comparison or change to \"IS NOT NULL\".";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage(messageWithIsNull)
            .next().atLine(4).withMessage(messageWithIsNotNull)
            .next().atLine(5).withMessage(messageWithIsNull)
            .next().atLine(6).withMessage(messageWithIsNotNull)
            .noMore();
    }

}
