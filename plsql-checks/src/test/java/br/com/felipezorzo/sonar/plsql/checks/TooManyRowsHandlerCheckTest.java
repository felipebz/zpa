package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class TooManyRowsHandlerCheckTest {

    @Test
    public void test() {
        TooManyRowsHandlerCheck check = new TooManyRowsHandlerCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/too_many_rows_handler.sql"), check);
        String message = "Fill this TOO_MANY_ROWS exception handler.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(7).withMessage(message)
            .noMore();
    }
    
}
