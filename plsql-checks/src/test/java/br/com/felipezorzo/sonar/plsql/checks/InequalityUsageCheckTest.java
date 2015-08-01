package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class InequalityUsageCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("inequality_usage_check.sql", new InequalityUsageCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage("Replace \"!=\" by \"<>\".")
            .next().atLine(4).withMessage("Replace \"^=\" by \"<>\".")
            .next().atLine(5).withMessage("Replace \"~=\" by \"<>\".")
            .noMore();
    }
    
}
