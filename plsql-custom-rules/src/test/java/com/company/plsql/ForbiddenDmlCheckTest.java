package com.company.plsql;

import com.sonar.sslr.impl.Parser;
import org.junit.Test;
import org.sonar.plsqlopen.checks.verifier.PlSqlCheckVerifier;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.sslr.AstNode;
import org.sonar.plugins.plsqlopen.api.sslr.Grammar;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ForbiddenDmlCheckTest {

@Test
public void test() {
    File file = new File("src/test/resources/forbidden-dml.sql");

    Parser<Grammar> parser = PlSqlParser.INSTANCE.create(
        new PlSqlConfiguration(StandardCharsets.UTF_8, true));

    AstNode rootTree = parser.parse(file);
    PlSqlVisitorContext context = new PlSqlVisitorContext(rootTree, null, null);

    ForbiddenDmlCheck check = new ForbiddenDmlCheck();
    List<PlSqlCheck.PreciseIssue> issues = check.scanFileForIssues(context);
}
    
}
