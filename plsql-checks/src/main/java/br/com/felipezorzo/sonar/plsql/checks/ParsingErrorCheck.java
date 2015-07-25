package br.com.felipezorzo.sonar.plsql.checks;

import java.io.StringWriter;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.NoSqale;
import org.sonar.squidbridge.checks.SquidCheck;

import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;

@Rule(
    key = ParsingErrorCheck.CHECK_KEY,
    priority = Priority.INFO, 
    name = "Parser failure"
)
@NoSqale
@ActivatedByDefault
public class ParsingErrorCheck extends SquidCheck<Grammar> implements AstScannerExceptionHandler {

    public static final String CHECK_KEY = "ParsingError";

    @Override
    public void processException(Exception e) {
      StringWriter exception = new StringWriter();
      getContext().createFileViolation(this, exception.toString());
    }

    @Override
    public void processRecognitionException(RecognitionException e) {
      getContext().createLineViolation(this, e.getMessage(), e.getLine());
    }
    
}
