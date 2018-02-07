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

import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.ce.measure.RangeDistributionBuilder;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plsqlopen.AnalyzerMessage;
import org.sonar.plsqlopen.DefaultPlSqlVisitorContext;
import org.sonar.plsqlopen.FormsMetadataAwareCheck;
import org.sonar.plsqlopen.PlSqlFile;
import org.sonar.plsqlopen.PlSqlVisitorContext;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.highlight.PlSqlHighlighterVisitor;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plsqlopen.metrics.ComplexityVisitor;
import org.sonar.plsqlopen.metrics.CpdVisitor;
import org.sonar.plsqlopen.metrics.FunctionComplexityVisitor;
import org.sonar.plsqlopen.metrics.MetricsVisitor;
import org.sonar.plsqlopen.parser.PlSqlParser;
import org.sonar.plsqlopen.symbols.DefaultTypeSolver;
import org.sonar.plsqlopen.symbols.SymbolVisitor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.impl.Parser;

public class PlSqlAstScanner {

    private static final Logger LOG = Loggers.get(PlSqlAstScanner.class);
    private static final Number[] LIMITS_COMPLEXITY_METHODS = {5, 10, 20, 30, 60, 90, 100};
    private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};
    
    private final SensorContext context;
    private final Parser<Grammar> parser;
    private final Collection<PlSqlCheck> checks;
    private final FormsMetadata formsMetadata;
    private NoSonarFilter noSonarFilter;

    public PlSqlAstScanner(SensorContext context, Collection<PlSqlCheck> checks, NoSonarFilter noSonarFilter, FormsMetadata formsMetadata, boolean isErrorRecoveryEnabled) {
        this.context = context;
        this.checks = checks;
        this.noSonarFilter = noSonarFilter;
        this.formsMetadata = formsMetadata;
        this.parser = PlSqlParser.create(new PlSqlConfiguration(context.fileSystem().encoding(), isErrorRecoveryEnabled));
    }
    
    @VisibleForTesting
    public Collection<AnalyzerMessage> scanFile(InputFile inputFile) {
        PlSqlFile plSqlFile = SonarQubePlSqlFile.create(inputFile, context);
        
        MetricsVisitor metricsVisitor = new MetricsVisitor();
        ComplexityVisitor complexityVisitor = new ComplexityVisitor();
        FunctionComplexityVisitor functionComplexityVisitor = new FunctionComplexityVisitor(); 

        List<PlSqlVisitor> checksToRun = new ArrayList<>();
        checksToRun.add(new SymbolVisitor(context, inputFile, new DefaultTypeSolver()));
        
        checks.stream()
              .filter(check -> formsMetadata != null || !(check instanceof FormsMetadataAwareCheck))
              .forEach(checksToRun::add);
        
        checksToRun.add(new PlSqlHighlighterVisitor(context, inputFile));
        checksToRun.add(metricsVisitor);
        checksToRun.add(complexityVisitor);
        checksToRun.add(functionComplexityVisitor);
        checksToRun.add(new CpdVisitor(context, inputFile));
        
        PlSqlAstWalker walker = new PlSqlAstWalker(checksToRun);
        
        PlSqlVisitorContext visitorContext;
        try {
            visitorContext = new DefaultPlSqlVisitorContext(parser.parse(plSqlFile.content()), plSqlFile, formsMetadata);
        } catch (RecognitionException e) {
            visitorContext = new DefaultPlSqlVisitorContext(plSqlFile, e, formsMetadata);
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
        
        noSonarFilter.noSonarInFile(inputFile, metricsVisitor.getLinesWithNoSonar());
        
        return visitorContext.getIssues();
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
