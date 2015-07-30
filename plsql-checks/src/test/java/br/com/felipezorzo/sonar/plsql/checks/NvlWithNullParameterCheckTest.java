package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class NvlWithNullParameterCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        NvlWithNullParameterCheck check = new NvlWithNullParameterCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(
                new File("src/test/resources/checks/nvl_with_null_parameter.sql"), check);
        final String message = "Remove the NULL parameter of this NVL.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .next().atLine(3).withMessage(message)
            .next().atLine(4).withMessage(message)
            .next().atLine(5).withMessage(message)
            .noMore();
    }
    
}
