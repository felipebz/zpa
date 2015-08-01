package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class InsertWithoutColumnsCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("insert_without_columns.sql", new InsertWithoutColumnsCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage("Specify the columns in this INSERT.")
            .noMore();
    }
    
}
