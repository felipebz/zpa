package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class DuplicatedValueInInCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("duplicated_value_in_in.sql", new DuplicatedValueInInCheck());
        final String message = "Remove or fix this duplicated value in the IN condition.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .next().atLine(7).withMessage(message)
            .noMore();
    }
    
}
