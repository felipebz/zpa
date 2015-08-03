package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class SelectWithRownumAndOrderByCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("select_with_rownum_and_order_by.sql", new SelectWithRownumAndOrderByCheck());
        final String message = "Move this ROWNUM comparation to a more external level to guarantee the ordering.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(5).withMessage(message)
            .next().atLine(11).withMessage(message)
            .noMore();
    }
    
}
