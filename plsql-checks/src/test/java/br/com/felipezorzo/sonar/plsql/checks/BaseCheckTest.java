package br.com.felipezorzo.sonar.plsql.checks;

import java.io.File;
import java.util.Locale;

import org.junit.Before;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceFile;

import com.sonar.sslr.api.Grammar;

import br.com.felipezorzo.sonar.plsql.PlSqlAstScanner;

public class BaseCheckTest {

    private final String defaultResourceFolder = "src/test/resources/checks/";
    
    @Before
    public void setUp() {
        Locale.setDefault(Locale.ENGLISH);
    }
    
    protected SourceFile scanSingleFile(String filename, SquidAstVisitor<Grammar> check) {
        return PlSqlAstScanner.scanSingleFile(new File(defaultResourceFolder + filename), check);
    }
    
}
