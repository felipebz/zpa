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
package org.sonar.plsqlopen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.source.Highlightable;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.highlight.PlSqlHighlighter;
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
import com.sonar.sslr.api.Grammar;

public class PlSqlSquidSensor implements Sensor {

    private static final Number[] LIMITS_COMPLEXITY_METHODS = {5, 10, 20, 30, 60, 90, 100};
    private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};
    
    private final PlSqlChecks checks;

    private SensorContext context;
    private AstScanner<Grammar> scanner;
    private FileSystem fileSystem;
    private ResourcePerspectives resourcePerspectives;
    private SonarComponents components;
    
    public PlSqlSquidSensor(FileSystem fileSystem, ResourcePerspectives perspectives,
            CheckFactory checkFactory, SonarComponents components) {
        this(fileSystem, perspectives, checkFactory, components, null);
    }

    public PlSqlSquidSensor(FileSystem fileSystem, ResourcePerspectives perspectives,
            CheckFactory checkFactory, SonarComponents components, @Nullable CustomPlSqlRulesDefinition[] customRulesDefinition) {
        this.checks = PlSqlChecks.createPlSqlCheck(checkFactory)
                .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
                .addCustomChecks(customRulesDefinition);
        this.fileSystem = fileSystem;
        this.resourcePerspectives = perspectives;
        this.components = components;
        components.setChecks(checks);
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        FilePredicates p = fileSystem.predicates();
        return fileSystem.hasFiles(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY)));
    }

    @Override
    public void analyse(@Nullable Project project, @Nullable SensorContext context) {
        this.context = context;

        List<SquidAstVisitor<Grammar>> visitors = new ArrayList<>();
        visitors.add(new SymbolVisitor());
        visitors.addAll(checks.all());
        
        this.scanner = PlSqlAstScanner.create(createConfiguration(), components, visitors);
        FilePredicates p = fileSystem.predicates();
        scanner.scanFiles(Lists.newArrayList(fileSystem.files(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY)))));

        Collection<SourceCode> squidSourceFiles = scanner.getIndex().search(new QueryByType(SourceFile.class));
        save(squidSourceFiles);        
    }

    private PlSqlConfiguration createConfiguration() {
        return new PlSqlConfiguration(fileSystem.encoding());
    }

    private void save(Collection<SourceCode> squidSourceFiles) {
        for (SourceCode squidSourceFile : squidSourceFiles) {
            SourceFile squidFile = (SourceFile) squidSourceFile;

            InputFile inputFile = fileSystem.inputFile(fileSystem.predicates()
                    .is(new java.io.File(squidFile.getKey())));

            saveFilesComplexityDistribution(inputFile, squidFile);
            saveFunctionsComplexityDistribution(inputFile, squidFile);
            saveMeasures(inputFile, squidFile);
            highlight(inputFile);
        }
    }

    private void saveMeasures(InputFile sonarFile, SourceFile squidFile) {
        context.saveMeasure(sonarFile, CoreMetrics.FILES, squidFile.getDouble(PlSqlMetric.FILES));
        context.saveMeasure(sonarFile, CoreMetrics.LINES, squidFile.getDouble(PlSqlMetric.LINES));
        context.saveMeasure(sonarFile, CoreMetrics.NCLOC, squidFile.getDouble(PlSqlMetric.LINES_OF_CODE));
        context.saveMeasure(sonarFile, CoreMetrics.COMMENT_LINES, squidFile.getDouble(PlSqlMetric.COMMENT_LINES));
        context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, squidFile.getDouble(PlSqlMetric.COMPLEXITY));
        context.saveMeasure(sonarFile, CoreMetrics.FUNCTIONS, squidFile.getDouble(PlSqlMetric.METHODS));
        context.saveMeasure(sonarFile, CoreMetrics.STATEMENTS, squidFile.getDouble(PlSqlMetric.STATEMENTS));
    }
    
    private void saveFunctionsComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
        Collection<SourceCode> squidFunctionsInFile = scanner.getIndex().search(new QueryByParent(squidFile),
                new QueryByType(SourceFunction.class));
        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(
                CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, LIMITS_COMPLEXITY_METHODS);
        for (SourceCode squidFunction : squidFunctionsInFile) {
            complexityDistribution.add(squidFunction.getDouble(PlSqlMetric.COMPLEXITY));
        }
        context.saveMeasure(sonarFile, complexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
    }

    private void saveFilesComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(
                CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, LIMITS_COMPLEXITY_FILES);
        complexityDistribution.add(squidFile.getDouble(PlSqlMetric.COMPLEXITY));
        context.saveMeasure(sonarFile, complexityDistribution.build().setPersistenceMode(PersistenceMode.MEMORY));
    }
    
    private void highlight(InputFile inputFile) {
        PlSqlHighlighter highlighter = new PlSqlHighlighter(createConfiguration());
        
        Highlightable perspective = resourcePerspectives.as(Highlightable.class, inputFile);
        
        if (perspective != null) {
            highlighter.highlight(perspective, inputFile.file());
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
