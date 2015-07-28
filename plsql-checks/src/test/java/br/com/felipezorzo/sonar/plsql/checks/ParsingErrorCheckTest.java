package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class ParsingErrorCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/parsing_error.sql"), new ParsingErrorCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(1)
            .noMore();
    }

}
