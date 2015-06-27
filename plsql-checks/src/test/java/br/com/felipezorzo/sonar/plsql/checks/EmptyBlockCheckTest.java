package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class EmptyBlockCheckTest {

    @Test
    public void matchesEmptyBlock() {
        EmptyBlockCheck check = new EmptyBlockCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/empty_block.sql"), check);
        String message = "Either remove or fill this block of code.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2)
            .withMessage(message).noMore();
    }
    
    @Test
    public void notMatchesABlockWithValidStatements() {
        EmptyBlockCheck check = new EmptyBlockCheck();
        SourceFile file = PlSqlAstScanner.scanSingleFile(new File("src/test/resources/checks/block_with_valid_statements.sql"), check);
        CheckMessagesVerifier.verify(file.getCheckMessages()).noMore();
    }

}
