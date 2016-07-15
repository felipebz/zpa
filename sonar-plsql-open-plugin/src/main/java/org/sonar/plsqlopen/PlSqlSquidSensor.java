/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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
package org.sonar.plsqlopen;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.ce.measure.RangeDistributionBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.highlight.PlSqlHighlighter;
import org.sonar.plsqlopen.lexer.PlSqlLexer;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.plugins.plsqlopen.api.PlSqlMetric;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;
import org.sonar.squidbridge.indexer.QueryByParent;
import org.sonar.squidbridge.indexer.QueryByType;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;

public class PlSqlSquidSensor implements Sensor {

    private static final Number[] LIMITS_COMPLEXITY_METHODS = {5, 10, 20, 30, 60, 90, 100};
    private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};
    
    private final PlSqlChecks checks;

    private AstScanner<Grammar> scanner;
    private SonarComponents components;
    private SensorContext context;
    private PlSqlConfiguration configuration;
    
    public PlSqlSquidSensor(CheckFactory checkFactory, SonarComponents components) {
        this(checkFactory, components, null);
    }

    public PlSqlSquidSensor(CheckFactory checkFactory, SonarComponents components,
            @Nullable CustomPlSqlRulesDefinition[] customRulesDefinition) {
        this.checks = PlSqlChecks.createPlSqlCheck(checkFactory)
                .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
                .addCustomChecks(customRulesDefinition);
        this.components = components;
        components.setChecks(checks);
    }
    
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("PlsqlSquidSensor")
            .onlyOnLanguage(PlSql.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        this.context = context;
        List<SquidAstVisitor<Grammar>> visitors = new ArrayList<>();
        visitors.add(new SymbolVisitor());
        visitors.addAll(checks.all());
        configuration = new PlSqlConfiguration(context.fileSystem().encoding());
        
        this.scanner = PlSqlAstScanner.create(configuration, components, visitors);
        FilePredicates p = context.fileSystem().predicates();
        scanner.scanFiles(Lists.newArrayList(context.fileSystem().files(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY)))));

        Collection<SourceCode> squidSourceFiles = scanner.getIndex().search(new QueryByType(SourceFile.class));
        save(squidSourceFiles);
        
        for (InputFile inputFile : context.fileSystem().inputFiles(p.and(p.hasType(InputFile.Type.TEST), p.hasLanguage(PlSql.KEY)))) {
            saveHighlighting(inputFile);
        }
    }

    private void save(Collection<SourceCode> squidSourceFiles) {
        for (SourceCode squidSourceFile : squidSourceFiles) {
            SourceFile squidFile = (SourceFile) squidSourceFile;

            InputFile inputFile = context.fileSystem().inputFile(context.fileSystem().predicates()
                    .is(new java.io.File(squidFile.getKey())));

            saveFilesComplexityDistribution(inputFile, squidFile);
            saveFunctionsComplexityDistribution(inputFile, squidFile);
            saveMeasures(inputFile, squidFile);
            saveHighlighting(inputFile);
            saveCpdTokens(inputFile);
        }
    }

    private void saveMeasures(InputFile sonarFile, SourceFile squidFile) {
        context.<Integer>newMeasure()
                .on(sonarFile)
                .forMetric(CoreMetrics.FILES)
                .withValue(squidFile.getInt(PlSqlMetric.FILES))
                .save();
        
        context.<Integer>newMeasure() .on(sonarFile)
                .forMetric(CoreMetrics.NCLOC)
                .withValue(squidFile.getInt(PlSqlMetric.LINES_OF_CODE))
                .save();
        
        context.<Integer>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.COMMENT_LINES)
                .withValue(squidFile.getInt(PlSqlMetric.COMMENT_LINES))
                .save();
        
        context.<Integer>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.COMPLEXITY)
                .withValue(squidFile.getInt(PlSqlMetric.COMPLEXITY))
                .save();
        
        context.<Integer>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.FUNCTIONS)
                .withValue(squidFile.getInt(PlSqlMetric.METHODS))
                .save();
        
        context.<Integer>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.STATEMENTS)
                .withValue(squidFile.getInt(PlSqlMetric.STATEMENTS))
                .save();
    }
    
    private void saveFunctionsComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
        Collection<SourceCode> squidFunctionsInFile = scanner.getIndex().search(new QueryByParent(squidFile),
                new QueryByType(SourceFunction.class));
        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_METHODS);
        for (SourceCode squidFunction : squidFunctionsInFile) {
            complexityDistribution.add(squidFunction.getDouble(PlSqlMetric.COMPLEXITY));
        }
        
        context.<String>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION)
                .withValue(complexityDistribution.build())
                .save();
    }

    private void saveFilesComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_FILES);
        complexityDistribution.add(squidFile.getDouble(PlSqlMetric.COMPLEXITY));
        context.<String>newMeasure().on(sonarFile)
                .forMetric(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION)
                .withValue(complexityDistribution.build())
                .save();
    }
    
    private void saveHighlighting(InputFile inputFile) {
        PlSqlHighlighter highlighter = new PlSqlHighlighter(configuration);
        highlighter.highlight(context, inputFile);
    }

    private void saveCpdTokens(InputFile inputFile) {
        NewCpdTokens newCpdTokens = context.newCpdTokens().onFile(inputFile);
        Lexer lexer = PlSqlLexer.create(configuration);
        String fileName = inputFile.absolutePath();
        List<Token> tokens = lexer.lex(new File(fileName));
        for (Token token : tokens) {
            if (token.getType() == GenericTokenType.EOF) {
                continue;
            }
            TokenLocation location = TokenLocation.from(token);
            newCpdTokens.addToken(location.line(), location.column(), location.endLine(), location.endColumn(), token.getValue());
        }
        newCpdTokens.save();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
