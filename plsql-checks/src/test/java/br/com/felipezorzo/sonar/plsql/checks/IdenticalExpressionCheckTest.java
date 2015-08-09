package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class IdenticalExpressionCheckTest extends BaseCheckTest {

	@Test
    public void test() {
        SourceFile file = scanSingleFile("identical_expression.sql", new IdenticalExpressionCheck());
        final String message = "Identical expressions on both sides of operator \"=\".";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(2).withMessage(message)
            .next().atLine(7).withMessage(message)
            .noMore();
    }
	
}
