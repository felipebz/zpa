package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class SelectAllColumnsCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("select_all_columns.sql", new SelectAllColumnsCheck());
        final String message = "SELECT * should not be used.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(3).withMessage(message)
            .next().atLine(7).withMessage(message)
            .next().atLine(11).withMessage(message)
            .next().atLine(15).withMessage(message)
            .noMore();
    }

}
