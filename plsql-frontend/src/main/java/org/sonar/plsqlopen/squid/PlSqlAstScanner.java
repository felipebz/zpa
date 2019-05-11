/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import static java.util.stream.Collectors.toList;

import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.FormsMetadataAwareCheck;
import org.sonar.plsqlopen.PlSqlChecks;
import org.sonar.plsqlopen.checks.IssueLocation;
import org.sonar.plsqlopen.highlight.PlSqlHighlighterVisitor;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.metrics.ComplexityVisitor;
import org.sonar.plsqlopen.metrics.CpdVisitor;
import org.sonar.plsqlopen.metrics.FunctionComplexityVisitor;
import org.sonar.plsqlopen.metrics.MetricsVisitor;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.symbols.DefaultTypeSolver;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.plugins.plsqlopen.api.PlSqlFile;
import org.sonar.plugins.plsqlopen.api.PlSqlVisitorContext;
import org.sonar.plugins.plsqlopen.api.annnotations.RuleInfo;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlCheck.PreciseIssue;
import org.sonar.plugins.plsqlopen.api.checks.PlSqlVisitor;
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public class PlSqlAstScanner {

    private static final Logger LOG = Loggers.get(PlSqlAstScanner.class);
    
    private final SensorContext context;
    private final Parser<Grammar> parser;
    private final Collection<PlSqlVisitor> checks;
    private final FormsMetadata formsMetadata;
    private NoSonarFilter noSonarFilter;
	private FileLinesContextFactory fileLinesContextFactory;
    private PlSqlChecks plsqlChecks;

    public PlSqlAstScanner(SensorContext context, Collection<PlSqlVisitor> checks, NoSonarFilter noSonarFilter, 
    		FormsMetadata formsMetadata, boolean isErrorRecoveryEnabled, FileLinesContextFactory fileLinesContextFactory) {
        this.context = context;
        this.checks = checks;
        this.noSonarFilter = noSonarFilter;
        this.formsMetadata = formsMetadata;
		this.fileLinesContextFactory = fileLinesContextFactory;
        this.parser = PlSqlParser.create(new PlSqlConfiguration(context.fileSystem().encoding(), isErrorRecoveryEnabled));
    }

    public PlSqlAstScanner(SensorContext context, PlSqlChecks checks, NoSonarFilter noSonarFilter, 
            FormsMetadata formsMetadata, boolean isErrorRecoveryEnabled, FileLinesContextFactory fileLinesContextFactory) {
        this(context, checks.all(), noSonarFilter, formsMetadata, isErrorRecoveryEnabled, fileLinesContextFactory);
        this.plsqlChecks = checks;
    }
    
    @VisibleForTesting
    public void scanFile(InputFile inputFile) {
        if (inputFile.type() == InputFile.Type.MAIN) {
            scanMainFile(inputFile);
        } else {
            scanTestFile(inputFile);
        }
    }

    public void scanMainFile(InputFile inputFile) {
        MetricsVisitor metricsVisitor = new MetricsVisitor();
        ComplexityVisitor complexityVisitor = new ComplexityVisitor();
        FunctionComplexityVisitor functionComplexityVisitor = new FunctionComplexityVisitor();

        PlSqlVisitorContext newVisitorContext = getPlSqlVisitorContext(inputFile);

        List<PlSqlVisitor> checksToRun = new ArrayList<>();
        checksToRun.add(new SymbolVisitor(context, inputFile, new DefaultTypeSolver()));
        
        checksToRun.addAll(
                checks.stream()
                    .filter(check -> formsMetadata != null || !(check instanceof FormsMetadataAwareCheck))
                    .filter(check -> check instanceof PlSqlCheck)
                    .filter(check -> ruleHasScope(check, RuleInfo.Scope.MAIN))
                    .collect(toList()));
        
        checksToRun.add(new PlSqlHighlighterVisitor(context, inputFile));
        checksToRun.add(metricsVisitor);
        checksToRun.add(complexityVisitor);
        checksToRun.add(functionComplexityVisitor);
        checksToRun.add(new CpdVisitor(context, inputFile));
        
        PlSqlAstWalker newWalker = new PlSqlAstWalker(checksToRun);
        newWalker.walk(newVisitorContext);
        
        noSonarFilter.noSonarInFile(inputFile, metricsVisitor.getLinesWithNoSonar());
        
        for (PlSqlVisitor check : checksToRun) {
            List<PreciseIssue> issues = ((PlSqlCheck)check).issues();
            if (!issues.isEmpty()) {
                saveIssues(inputFile, check, issues);
            }
        }
        
        saveMetricOnFile(inputFile, CoreMetrics.STATEMENTS, metricsVisitor.getNumberOfStatements());
        saveMetricOnFile(inputFile, CoreMetrics.NCLOC, metricsVisitor.getLinesOfCode().size());
        saveMetricOnFile(inputFile, CoreMetrics.COMMENT_LINES, metricsVisitor.getLinesOfComments().size());
        saveMetricOnFile(inputFile, CoreMetrics.COMPLEXITY, complexityVisitor.getComplexity());
        saveMetricOnFile(inputFile, CoreMetrics.FUNCTIONS, functionComplexityVisitor.getNumberOfFunctions());
        
        if (fileLinesContextFactory != null) {
            FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
            for (int line : metricsVisitor.getExecutableLines()) {
                fileLinesContext.setIntValue(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, line, 1);
            }
            fileLinesContext.save();
        }
    }

    public void scanTestFile(InputFile inputFile) {
        PlSqlVisitorContext newVisitorContext = getPlSqlVisitorContext(inputFile);
        MetricsVisitor metricsVisitor = new MetricsVisitor();

        List<PlSqlVisitor> checksToRun = new ArrayList<>();
        checksToRun.add(new SymbolVisitor(context, inputFile, new DefaultTypeSolver()));
        checksToRun.add(new PlSqlHighlighterVisitor(context, inputFile));
        checksToRun.add(metricsVisitor);

        checksToRun.addAll(
            checks.stream()
                .filter(check -> check instanceof PlSqlCheck)
                .filter(check -> ruleHasScope(check, RuleInfo.Scope.TEST))
                .collect(toList()));

        PlSqlAstWalker newWalker = new PlSqlAstWalker(checksToRun);
        newWalker.walk(newVisitorContext);

        noSonarFilter.noSonarInFile(inputFile, metricsVisitor.getLinesWithNoSonar());

        for (PlSqlVisitor check : checksToRun) {
            List<PreciseIssue> issues = ((PlSqlCheck)check).issues();
            if (!issues.isEmpty()) {
                saveIssues(inputFile, check, issues);
            }
        }
    }

    private boolean ruleHasScope(PlSqlVisitor check, RuleInfo.Scope scope) {
        RuleInfo ruleInfo = AnnotationUtils.getAnnotation(check, RuleInfo.class);
        if (ruleInfo != null) {
            return ruleInfo.scope() == RuleInfo.Scope.ALL ||
                ruleInfo.scope() == scope;
        }
        return scope == RuleInfo.Scope.MAIN;
    }

    private PlSqlVisitorContext getPlSqlVisitorContext(InputFile inputFile) {
        PlSqlFile plSqlFile = SonarQubePlSqlFile.create(inputFile);

        PlSqlVisitorContext visitorContext;
        try {
            AstNode root = getSemanticNode(parser.parse(plSqlFile.contents()));
            visitorContext = new PlSqlVisitorContext(root, plSqlFile, formsMetadata);
        } catch (RecognitionException e) {
            visitorContext = new PlSqlVisitorContext(plSqlFile, e, formsMetadata);
            LOG.error("Unable to parse file: " + inputFile.toString());
            LOG.error(e.getMessage());
        } catch (Exception e) {
            checkInterrupted(e);
            throw new AnalysisException("Unable to analyze file: " + inputFile.toString(), e);
        } catch (Throwable e) {
            throw new AnalysisException("Unable to analyze file: " + inputFile.toString(), e);
        }
        return visitorContext;
    }

    public static SemanticAstNode getSemanticNode(AstNode node) {
        SemanticAstNode annotatedNode = new SemanticAstNode(node);


        for (AstNode child : node.getChildren()) {
            annotatedNode.addChild(getSemanticNode(child));
        }

        return annotatedNode;
    }
    
    private void saveIssues(InputFile inputFile, PlSqlVisitor check, List<PreciseIssue> issues) {
        RuleKey ruleKey = plsqlChecks.ruleKey(check);
        for (PreciseIssue preciseIssue : issues) {

            NewIssue newIssue = context.newIssue().forRule(ruleKey);

            Integer cost = preciseIssue.cost();
            if (cost != null) {
                newIssue.gap(cost.doubleValue());
            }

            newIssue.at(newLocation(inputFile, newIssue, preciseIssue.primaryLocation()));

            for (IssueLocation secondaryLocation : preciseIssue.secondaryLocations()) {
                newIssue.addLocation(newLocation(inputFile, newIssue, secondaryLocation));
            }

            newIssue.save();
        }
    }

    private static NewIssueLocation newLocation(InputFile inputFile, NewIssue issue, IssueLocation location) {
        NewIssueLocation newLocation = issue.newLocation().on(inputFile);
        if (location.startLine() != IssueLocation.UNDEFINED_LINE) {
            TextRange range;
            if (location.startLineOffset() == IssueLocation.UNDEFINED_OFFSET) {
                range = inputFile.selectLine(location.startLine());
            } else {
                range = inputFile.newRange(location.startLine(), location.startLineOffset(), location.endLine(),
                        location.endLineOffset());
            }
            newLocation.at(range);
        }

        newLocation.message(location.message());
        return newLocation;
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
    
}
