/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015 Felipe Zorzo
 * felipe.b.zorzo@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package br.com.felipezorzo.sonar.plsql.squid;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.AstScanner.Builder;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.metrics.CommentsVisitor;
import org.sonar.squidbridge.metrics.ComplexityVisitor;
import org.sonar.squidbridge.metrics.LinesVisitor;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.impl.Parser;

import br.com.felipezorzo.sonar.plsql.api.PlSqlGrammar;
import br.com.felipezorzo.sonar.plsql.api.PlSqlKeyword;
import br.com.felipezorzo.sonar.plsql.api.PlSqlMetric;
import br.com.felipezorzo.sonar.plsql.parser.PlSqlParser;

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
        AstScanner<Grammar> scanner = create(new PlSqlConfiguration(StandardCharsets.UTF_8), visitors);
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
        setMetrics(conf, builder);

        /* External visitors (typically Check ones) */
        for (SquidAstVisitor<Grammar> visitor : visitors) {
            if (visitor instanceof CharsetAwareVisitor) {
                ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
            }
            builder.withSquidAstVisitor(visitor);
        }

        return builder.build();
    }
    
    private static void setMetrics(PlSqlConfiguration conf, Builder<Grammar> builder) {
        builder.withSquidAstVisitor(new LinesVisitor<>(PlSqlMetric.LINES));
        builder.withSquidAstVisitor(new PlSqlLinesOfCodeVisitor(PlSqlMetric.LINES_OF_CODE));
        builder.withSquidAstVisitor(CommentsVisitor.<Grammar>builder().withCommentMetric(PlSqlMetric.COMMENT_LINES)
                .withNoSonar(true)
                .withIgnoreHeaderComment(conf.getIgnoreHeaderComments())
                .build());
        
        AstNodeType[] complexityAstNodeType = new AstNodeType[] {
                PlSqlGrammar.CREATE_PROCEDURE,
                PlSqlGrammar.CREATE_FUNCTION,
                PlSqlGrammar.ANONYMOUS_BLOCK,
                
                PlSqlGrammar.PROCEDURE_DECLARATION,
                PlSqlGrammar.FUNCTION_DECLARATION,
                
                PlSqlGrammar.LOOP_STATEMENT,
                PlSqlGrammar.CONTINUE_STATEMENT,
                PlSqlGrammar.FOR_STATEMENT,
                PlSqlGrammar.EXIT_STATEMENT,
                PlSqlGrammar.IF_STATEMENT,
                PlSqlGrammar.RAISE_STATEMENT,
                PlSqlGrammar.RETURN_STATEMENT,
                PlSqlGrammar.WHILE_STATEMENT,
                
                // this includes WHEN in exception handlers, exit/continue statements and CASE expressions
                PlSqlKeyword.WHEN,
                PlSqlKeyword.ELSIF
        };
        builder.withSquidAstVisitor(ComplexityVisitor.<Grammar> builder().setMetricDef(PlSqlMetric.COMPLEXITY)
                .subscribeTo(complexityAstNodeType).build());
    }

    private static void setCommentAnalyser(AstScanner.Builder<Grammar> builder) {
        builder.setCommentAnalyser(new PlSqlCommentAnalyzer());
    }
}
