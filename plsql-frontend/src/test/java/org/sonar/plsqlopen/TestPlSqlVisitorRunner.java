package org.sonar.plsqlopen;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;

import com.google.common.io.Files;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class TestPlSqlVisitorRunner {

    private TestPlSqlVisitorRunner() {
    }

    public static void scanFile(File file, PlSqlVisitor... visitors) {
        PlSqlVisitorContext context = createContext(file);
        for (PlSqlVisitor visitor : visitors) {
            visitor.scanFile(context);
        }
    }

    public static PlSqlVisitorContext createContext(File file) {
        Parser<Grammar> parser = PlSqlParser.create(new PlSqlConfiguration(StandardCharsets.UTF_8));
        TestPlSqlFile plSqlFile = new TestPlSqlFile(file);
        AstNode rootTree = parser.parse(plSqlFile.content());
        return new DefaultPlSqlVisitorContext<>(rootTree, plSqlFile, null);
    }

    private static class TestPlSqlFile implements PlSqlFile {

        private final File file;

        public TestPlSqlFile(File file) {
            this.file = file;
        }

        @Override
        public String content() {
            try {
                return Files.toString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + file, e);
            }
        }

        @Override
        public String fileName() {
            return file.getName();
        }

        @Override
        public File file() {
            return file;
        }

    }

}
