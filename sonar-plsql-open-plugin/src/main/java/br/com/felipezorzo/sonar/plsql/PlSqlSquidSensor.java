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
package br.com.felipezorzo.sonar.plsql;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.source.Highlightable;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.api.SourceFunction;
import org.sonar.squidbridge.indexer.QueryByParent;
import org.sonar.squidbridge.indexer.QueryByType;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Grammar;

import br.com.felipezorzo.sonar.plsql.api.PlSqlMetric;
import br.com.felipezorzo.sonar.plsql.checks.CheckList;
import br.com.felipezorzo.sonar.plsql.highlight.PlSqlHighlighter;
import br.com.felipezorzo.sonar.plsql.squid.PlSqlAstScanner;
import br.com.felipezorzo.sonar.plsql.squid.PlSqlConfiguration;

public class PlSqlSquidSensor implements Sensor {

    private static final Number[] LIMITS_COMPLEXITY_METHODS = {1, 2, 4, 6, 8, 10, 12};
    private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};
    
    private final Checks<SquidAstVisitor<Grammar>> checks;

    private SensorContext context;
    private AstScanner<Grammar> scanner;
    private FileSystem fileSystem;
    private ResourcePerspectives resourcePerspectives;

    public PlSqlSquidSensor(FileSystem fileSystem, ResourcePerspectives perspectives,
            CheckFactory checkFactory) {
        this.checks = checkFactory.<SquidAstVisitor<Grammar>> create(
                CheckList.REPOSITORY_KEY).addAnnotatedChecks(
                CheckList.getChecks());
        this.fileSystem = fileSystem;
        this.resourcePerspectives = perspectives;
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        FilePredicates p = fileSystem.predicates();
        return fileSystem.hasFiles(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY)));
    }

    @Override
    public void analyse(@Nullable Project project, @Nullable SensorContext context) {
        this.context = context;

        List<SquidAstVisitor<Grammar>> visitors = Lists.newArrayList(checks.all());
        this.scanner = PlSqlAstScanner.create(createConfiguration(), visitors);
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
            saveIssues(inputFile, squidFile);
            highlight(inputFile);
        }
    }

    private void saveMeasures(InputFile sonarFile, SourceFile squidFile) {
        context.saveMeasure(sonarFile, CoreMetrics.FILES, squidFile.getDouble(PlSqlMetric.FILES));
        context.saveMeasure(sonarFile, CoreMetrics.LINES, squidFile.getDouble(PlSqlMetric.LINES));
        context.saveMeasure(sonarFile, CoreMetrics.NCLOC, squidFile.getDouble(PlSqlMetric.LINES_OF_CODE));
        context.saveMeasure(sonarFile, CoreMetrics.COMMENT_LINES, squidFile.getDouble(PlSqlMetric.COMMENT_LINES));
        context.saveMeasure(sonarFile, CoreMetrics.COMPLEXITY, squidFile.getDouble(PlSqlMetric.COMPLEXITY));
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
    
    private void saveIssues(InputFile sonarFile, SourceFile squidFile) {
        Collection<CheckMessage> messages = squidFile.getCheckMessages();
        for (CheckMessage message : messages) {
            @SuppressWarnings("unchecked")
            RuleKey ruleKey = checks.ruleKey((SquidAstVisitor<Grammar>) message.getCheck());
            Issuable issuable = resourcePerspectives.as(Issuable.class, sonarFile);

            if (issuable != null) {
                Issue issue = issuable.newIssueBuilder().ruleKey(ruleKey)
                        .line(message.getLine())
                        .message(message.getText(Locale.ENGLISH)).build();
                issuable.addIssue(issue);
            }
        }
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
