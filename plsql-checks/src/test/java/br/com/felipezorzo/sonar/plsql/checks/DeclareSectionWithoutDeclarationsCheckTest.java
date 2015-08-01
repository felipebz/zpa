package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class DeclareSectionWithoutDeclarationsCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        SourceFile file = scanSingleFile("declare_section_without_declarations.sql", new DeclareSectionWithoutDeclarationsCheck());
        String message = "Remove this DECLARE keyword.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .noMore();
    }
    
}
