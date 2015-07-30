package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class DeclareSectionWithoutDeclarationsCheckTest extends BaseCheckTest {
    
    @Test
    public void test() {
        DeclareSectionWithoutDeclarationsCheck check = new DeclareSectionWithoutDeclarationsCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/declare_section_without_declarations.sql"), check);
        String message = "Remove this DECLARE keyword.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .noMore();
    }
    
}
