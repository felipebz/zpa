package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class InequalityUsageCheckTest {

    @Test
    public void matchesInvalidInequalityUsage() {
        InequalityUsageCheck check = new InequalityUsageCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(
                new File("src/test/resources/checks/inequality_usage_check.sql"), check);
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage("Replace \"!=\" by \"<>\"")
            .next().atLine(4).withMessage("Replace \"^=\" by \"<>\"")
            .next().atLine(5).withMessage("Replace \"~=\" by \"<>\"")
            .noMore();
    }
    
}
