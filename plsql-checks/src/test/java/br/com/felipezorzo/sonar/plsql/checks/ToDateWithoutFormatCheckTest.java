package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ToDateWithoutFormatCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("to_date_without_format.sql", new ToDateWithoutFormatCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage("Specify the date format in this TO_DATE.")
            .noMore();
    }
    
}
