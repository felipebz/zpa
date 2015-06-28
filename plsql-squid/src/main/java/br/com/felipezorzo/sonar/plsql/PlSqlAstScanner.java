package br.com.felipezorzo.sonar.plsql;

import java.io.File;
import java.util.Collection;

import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.CommentAnalyser;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;

import br.com.felipezorzo.sonar.plsql.api.PlSqlMetric;
import br.com.felipezorzo.sonar.plsql.parser.PlSqlParser;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

public class PlSqlAstScanner {

    private PlSqlAstScanner() {
    }
    
    public static SourceFile scanSingleFile(File file, SquidAstVisitor<Grammar> visitor) {
        return scanSingleFile(file, ImmutableList.of(visitor));
    }

    public static SourceFile scanSingleFile(File file, Collection<SquidAstVisitor<Grammar>> visitors) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("File '" + file + "' not found.");
        }
        AstScanner<Grammar> scanner = create(new PlSqlConfiguration(Charsets.UTF_8), visitors);
        scanner.scanFile(file);
        Collection<SourceCode> sources = scanner.getIndex().search(new QueryByType(SourceFile.class));
        if (sources.size() != 1) {
            throw new IllegalStateException("Only one SourceFile was expected whereas "
                            + sources.size() + " has been returned.");
        }
        return (SourceFile) sources.iterator().next();
    }
    
    public static AstScanner<Grammar> create(PlSqlConfiguration conf, Collection<SquidAstVisitor<Grammar>> visitors) {
        final SquidAstVisitorContextImpl<Grammar> context = 
                new SquidAstVisitorContextImpl<>(new SourceProject("PL/SQL Project"));
        final Parser<Grammar> parser = PlSqlParser.create(conf);

        AstScanner.Builder<Grammar> builder = AstScanner.<Grammar>builder(
                context).setBaseParser(parser);
        
        builder.setFilesMetric(PlSqlMetric.FILES);
        setCommentAnalyser(builder);

        /* External visitors (typically Check ones) */
        for (SquidAstVisitor<Grammar> visitor : visitors) {
            if (visitor instanceof CharsetAwareVisitor) {
                ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
            }
            builder.withSquidAstVisitor(visitor);
        }

        return builder.build();
    }
    
    private static void setCommentAnalyser(AstScanner.Builder<Grammar> builder) {
        builder.setCommentAnalyser(new CommentAnalyser() {
            @Override
            public boolean isBlank(String line) {
                for (int i = 0; i < line.length(); i++) {
                    if (Character.isLetterOrDigit(line.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String getContents(String comment) {
                if (comment.startsWith("--")) {
                    return comment.substring(2);
                } else if (comment.startsWith("/*")) {
                    if (comment.endsWith("*/")) {
                        return comment.substring(2, comment.length() - 2);
                    }
                    return comment.substring(2);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        });
    }
}
