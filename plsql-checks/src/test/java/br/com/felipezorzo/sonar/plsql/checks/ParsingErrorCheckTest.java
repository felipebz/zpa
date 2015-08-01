package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ParsingErrorCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("parsing_error.sql", new ParsingErrorCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(1)
            .noMore();
    }

}
