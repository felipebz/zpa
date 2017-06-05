/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen.squid;

import java.io.File;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.ce.measure.RangeDistributionBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.DefaultPlSqlVisitorContext;
import org.sonar.plsqlopen.FormsMetadataAwareCheck;
import org.sonar.plsqlopen.PlSqlFile;
import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.plsqlopen.SonarComponents;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.metrics.ComplexityVisitor;
import org.sonar.plsqlopen.metrics.FunctionComplexityVisitor;
import org.sonar.plsqlopen.metrics.MetricsVisitor;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.symbols.SymbolHighlighter;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.PlSqlKeyword;
import org.sonar.plugins.plsqlopen.api.PlSqlMetric;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.AstScanner.Builder;
import org.sonar.squidbridge.SourceCodeBuilderVisitor;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.AnalysisException;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;
import org.sonar.squidbridge.api.SourceProject;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.metrics.CommentsVisitor;
import org.sonar.squidbridge.metrics.CounterVisitor;
import org.sonar.squidbridge.metrics.LinesVisitor;

import com.google.common.base.Throwables;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public class PlSqlAstScanner {

    private static final Logger LOG = Loggers.get(PlSqlAstScanner.class);
    private static final Number[] LIMITS_COMPLEXITY_METHODS = {5, 10, 20, 30, 60, 90, 100};
    private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};
    
    private final SensorContext context;
    private final Parser<Grammar> parser;
    private final List<InputFile> inputFiles;
    private final Collection<PlSqlCheck> checks;
    private final SonarComponents components;
    private final ProgressReport progressReport = new ProgressReport("Report about progress of code analyzer", TimeUnit.SECONDS.toMillis(10));

    public PlSqlAstScanner(SensorContext context, Collection<PlSqlCheck> checks, List<InputFile> inputFiles, SonarComponents components) {
      this.context = context;
      this.checks = checks;
      this.inputFiles = inputFiles;
      this.parser = PlSqlParser.create(new PlSqlConfiguration(context.fileSystem().encoding()));
      this.components = components;
    }

    public void scanFiles() {
        progressReport.start(inputFiles);
        for (InputFile plSqlFile : inputFiles) {
            scanFile(plSqlFile);
            progressReport.nextFile();
        }
        progressReport.stop();
    }

    private void scanFile(InputFile inputFile) {
        PlSqlFile plSqlFile = SonarQubePlSqlFile.create(inputFile, context);
        
        MetricsVisitor metricsVisitor = new MetricsVisitor();
        ComplexityVisitor complexityVisitor = new ComplexityVisitor();
        FunctionComplexityVisitor functionComplexityVisitor = new FunctionComplexityVisitor(); 
        
        List<PlSqlCheck> checksToRun = new ArrayList<>(checks);
        checksToRun.add(metricsVisitor);
        checksToRun.add(complexityVisitor);
        checksToRun.add(functionComplexityVisitor);
        
        PlSqlAstWalker walker = new PlSqlAstWalker(checksToRun);
        
        PlSqlVisitorContext visitorContext;
        try {
            visitorContext = new DefaultPlSqlVisitorContext<>(parser.parse(plSqlFile.content()), plSqlFile, components);
        } catch (RecognitionException e) {
            visitorContext = new DefaultPlSqlVisitorContext<>(plSqlFile, e, components);
            LOG.error("Unable to parse file: " + inputFile.absolutePath());
            LOG.error(e.getMessage());
        } catch (Exception e) {
            checkInterrupted(e);
            throw new AnalysisException("Unable to analyze file: " + inputFile.absolutePath(), e);
        } catch (Throwable e) {
            throw new AnalysisException("Unable to analyze file: " + inputFile.absolutePath(), e);
        }
        
        try {
            walker.walk(visitorContext);
        } catch (Throwable e) {
            throw new AnalysisException("Unable to analyze file: " + inputFile.absolutePath(), e);
        }
        
        saveMetricOnFile(inputFile, CoreMetrics.STATEMENTS, metricsVisitor.getNumberOfStatements());
        saveMetricOnFile(inputFile, CoreMetrics.NCLOC, metricsVisitor.getLinesOfCode().size());
        saveMetricOnFile(inputFile, CoreMetrics.COMMENT_LINES, metricsVisitor.getLinesOfComments().size());
        saveMetricOnFile(inputFile, CoreMetrics.COMPLEXITY, complexityVisitor.getComplexity());
        saveMetricOnFile(inputFile, CoreMetrics.FUNCTIONS, functionComplexityVisitor.getNumberOfFunctions());
        
        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_FILES);
        complexityDistribution.add(complexityVisitor.getComplexity());
        saveMetricOnFile(inputFile, CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, complexityDistribution.build());
        
        RangeDistributionBuilder functionComplexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_METHODS);
        for (Integer functionComplexity : functionComplexityVisitor.getFunctionComplexities()) {
            functionComplexityDistribution.add(functionComplexity);
        }
        saveMetricOnFile(inputFile, CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, functionComplexityDistribution.build());
    }
    
    private static void checkInterrupted(Exception e) {
        Throwable cause = Throwables.getRootCause(e);
        if (cause instanceof InterruptedException || cause instanceof InterruptedIOException) {
            throw new AnalysisException("Analysis cancelled", e);
        }
    }

    private <T extends Serializable> void saveMetricOnFile(InputFile inputFile, Metric<T> metric, T value) {
        context.<T>newMeasure()
            .on(inputFile)
            .forMetric(metric)
            .withValue(value)
            .save();
    }
    
//    public static AstScanner<Grammar> create(PlSqlConfiguration conf, SonarComponents components, Collection<PlSqlCheck> visitors) {
//        final SquidAstVisitorContextImpl<Grammar> context = 
//                new DefaultPlSqlVisitorContext<>(new SourceProject("PL/SQL Project"), components);
//        final Parser<Grammar> parser = PlSqlParser.create(conf);
//
//        AstScanner.Builder<Grammar> builder = new ProgressAstScanner.Builder<>(
//                context).setBaseParser(parser);
//        
//        builder.withMetrics(PlSqlMetric.values());
//        builder.setFilesMetric(PlSqlMetric.FILES);
//        setMethodAnalyser(builder);
//        setCommentAnalyser(builder);
//        setMetrics(builder);
//
//        /* External visitors (typically Check ones) */
//        if (visitors != null) {
//            for (PlSqlCheck visitor : visitors) {
//                if (visitor instanceof CharsetAwareVisitor) {
//                    ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
//                }
//                
//                if (!(visitor instanceof FormsMetadataAwareCheck) || components.getFormsMetadata() != null) {
//                    //builder.withSquidAstVisitor(visitor);
//                }
//            }
//        }
//
//        return builder.build();
//    }
//    
//    private static void setMetrics(Builder<Grammar> builder) {
//        builder.withSquidAstVisitor(new LinesVisitor<>(PlSqlMetric.LINES));
//        builder.withSquidAstVisitor(new PlSqlLinesOfCodeVisitor(PlSqlMetric.LINES_OF_CODE));
//        builder.withSquidAstVisitor(CommentsVisitor.<Grammar>builder().withCommentMetric(PlSqlMetric.COMMENT_LINES)
//                .withNoSonar(true)
//                .build());
//        
//        builder.withSquidAstVisitor(CounterVisitor.<Grammar>builder()
//                .setMetricDef(PlSqlMetric.STATEMENTS)
//                .subscribeTo(PlSqlGrammar.STATEMENT)
//                .build());
//        
//        AstNodeType[] complexityAstNodeType = new AstNodeType[] {
//                PlSqlGrammar.CREATE_PROCEDURE,
//                PlSqlGrammar.CREATE_FUNCTION,
//                PlSqlGrammar.ANONYMOUS_BLOCK,
//                
//                PlSqlGrammar.PROCEDURE_DECLARATION,
//                PlSqlGrammar.FUNCTION_DECLARATION,
//                
//                PlSqlGrammar.LOOP_STATEMENT,
//                PlSqlGrammar.CONTINUE_STATEMENT,
//                PlSqlGrammar.FOR_STATEMENT,
//                PlSqlGrammar.EXIT_STATEMENT,
//                PlSqlGrammar.IF_STATEMENT,
//                PlSqlGrammar.RAISE_STATEMENT,
//                PlSqlGrammar.RETURN_STATEMENT,
//                PlSqlGrammar.WHILE_STATEMENT,
//                
//                // this includes WHEN in exception handlers, exit/continue statements and CASE expressions
//                PlSqlKeyword.WHEN,
//                PlSqlKeyword.ELSIF
//        };
//        builder.withSquidAstVisitor(ComplexityVisitor.<Grammar> builder().setMetricDef(PlSqlMetric.COMPLEXITY)
//                .subscribeTo(complexityAstNodeType).build());
//    }
//    
//    private static void setMethodAnalyser(AstScanner.Builder<Grammar> builder) {
//        PlSqlGrammar[] methodDeclarations = { 
//                PlSqlGrammar.CREATE_PROCEDURE,
//                PlSqlGrammar.CREATE_FUNCTION, 
//                PlSqlGrammar.PROCEDURE_DECLARATION,
//                PlSqlGrammar.FUNCTION_DECLARATION };
//        
//        builder.withSquidAstVisitor(new SourceCodeBuilderVisitor<>(
//        	(parentSourceCode, astNode) -> {
//                String functionName = astNode.getFirstChild(PlSqlGrammar.UNIT_NAME, PlSqlGrammar.IDENTIFIER_NAME).getTokenValue();
//                SourceFunction function = new SourceFunction(functionName + ":" + astNode.getToken().getLine());
//                function.setStartAtLine(astNode.getTokenLine());
//                return function;
//        }, methodDeclarations));
//
//        builder.withSquidAstVisitor(CounterVisitor.<Grammar>builder()
//                .setMetricDef(PlSqlMetric.METHODS)
//                .subscribeTo(methodDeclarations)
//                .build());
//    }
//
//    private static void setCommentAnalyser(AstScanner.Builder<Grammar> builder) {
//        builder.setCommentAnalyser(new PlSqlCommentAnalyzer());
//    }
}
