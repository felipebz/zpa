package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class TooManyRowsHandlerCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("too_many_rows_handler.sql", new TooManyRowsHandlerCheck());
        String message = "Fill this TOO_MANY_ROWS exception handler.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(7).withMessage(message)
            .noMore();
    }
    
}
