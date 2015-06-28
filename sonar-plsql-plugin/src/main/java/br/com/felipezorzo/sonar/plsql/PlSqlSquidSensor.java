package br.com.felipezorzo.sonar.plsql;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.squidbridge.AstScanner;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.indexer.QueryByType;

import br.com.felipezorzo.sonar.plsql.api.PlSqlMetric;
import br.com.felipezorzo.sonar.plsql.checks.CheckList;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.Grammar;

public class PlSqlSquidSensor implements Sensor {

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
    public void analyse(Project project, SensorContext context) {
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

            saveMeasures(inputFile, squidFile);
            saveIssues(inputFile, squidFile);
        }
    }

    private void saveMeasures(InputFile sonarFile, SourceFile squidFile) {
        context.saveMeasure(sonarFile, CoreMetrics.FILES,
                squidFile.getDouble(PlSqlMetric.FILES));
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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
