package br.com.felipezorzo.sonar.plsql.checks;

import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class ExplicitInParameterCheckTest extends BaseCheckTest {

    @Test
    public void test() {
        SourceFile file = scanSingleFile("explicit_in_parameter.sql", new ExplicitInParameterCheck());
        final String message = "Explicitly declare this parameter as IN.";
        CheckMessagesVerifier.verify(file.getCheckMessages())
            .next().atLine(1).withMessage(message)
            .noMore();
    }
    
}
