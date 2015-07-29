package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class InsertWithoutColumnsCheckTest extends BaseCheckTest {

    @Test
    public void matchesInsertWithoutColumns() {
        InsertWithoutColumnsCheck check = new InsertWithoutColumnsCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(
                new File("src/test/resources/checks/insert_without_columns.sql"), check);
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage("Specify the columns in this INSERT.")
            .noMore();
    }
    
}
