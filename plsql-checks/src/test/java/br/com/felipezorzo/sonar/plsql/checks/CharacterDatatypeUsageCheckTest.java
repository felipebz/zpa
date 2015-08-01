package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class CharacterDatatypeUsageCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("character_datatype_usage.sql", new CharacterDatatypeUsageCheck());
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage("Use VARCHAR2 instead of VARCHAR.")
            .next().atLine(3).withMessage("Use VARCHAR2 instead of CHAR.")
            .noMore();
    }
    
}
