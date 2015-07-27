package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class ComparisonWithNullCheckTest {
    
    @Test
    public void test() {
        ComparisonWithNullCheck check = new ComparisonWithNullCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/comparison_with_null.sql"), check);
        String message = "Change this comparison to \"IS NULL\" or \"IS NOT NULL\".";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage(message)
            .next().atLine(4).withMessage(message)
            .next().atLine(5).withMessage(message)
            .next().atLine(6).withMessage(message)
            .noMore();
    }

}
