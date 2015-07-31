package br.com.felipezorzo.sonar.plsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;

import br.com.felipezorzo.sonar.plsql.checks.CheckList;

public class PlSqlSquidSensorTest {

    private PlSqlSquidSensor sensor;
    private DefaultFileSystem fs = new DefaultFileSystem(new File("."));
    ResourcePerspectives perspectives;

    @Before
    public void setUp() {
      FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
      FileLinesContext fileLinesContext = mock(FileLinesContext.class);
      when(fileLinesContextFactory.createFor(Mockito.any(InputFile.class))).thenReturn(fileLinesContext);
      ActiveRules activeRules = (new ActiveRulesBuilder())
          .create(RuleKey.of(CheckList.REPOSITORY_KEY, "EmptyBlock"))
          .setName("Print Statement Usage")
          .activate()
          .build();
      CheckFactory checkFactory = new CheckFactory(activeRules);
      perspectives = mock(ResourcePerspectives.class);
      sensor = new PlSqlSquidSensor(fileLinesContextFactory, fs, perspectives, checkFactory);
    }
    
    @Test
    public void shouldExecuteInPlSqlProject() {
      Project project = mock(Project.class);
      assertThat(sensor.toString()).isEqualTo("PlSqlSquidSensor");
      assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
      fs.add(new DefaultInputFile("test.sql").setLanguage(PlSql.KEY));
      assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    }
    
    @Test
    public void shouldAnalyse() {
      String relativePath = "src/test/resources/br/com/felipezorzo/sonar/plsql/code.sql";
      DefaultInputFile inputFile = new DefaultInputFile(relativePath).setLanguage(PlSql.KEY);
      inputFile.setAbsolutePath((new File(relativePath)).getAbsolutePath());
      fs.add(inputFile);

      Issuable issuable = mock(Issuable.class);
      Issuable.IssueBuilder issueBuilder = mock(Issuable.IssueBuilder.class);
      when(perspectives.as(Mockito.eq(Issuable.class), Mockito.any(InputFile.class))).thenReturn(issuable);
      when(issuable.newIssueBuilder()).thenReturn(issueBuilder);
      when(issueBuilder.ruleKey(Mockito.any(RuleKey.class))).thenReturn(issueBuilder);
      when(issueBuilder.line(Mockito.any(Integer.class))).thenReturn(issueBuilder);
      when(issueBuilder.message(Mockito.any(String.class))).thenReturn(issueBuilder);

      Project project = new Project("key");
      SensorContext context = mock(SensorContext.class);
      sensor.analyse(project, context);

      verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.FILES), Mockito.eq(1.0));
      verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.LINES), Mockito.eq(16.0));
      verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.NCLOC), Mockito.eq(9.0));
      verify(context).saveMeasure(Mockito.any(InputFile.class), Mockito.eq(CoreMetrics.COMMENT_LINES), Mockito.eq(2.0));
      verify(issuable).addIssue(Mockito.any(Issue.class));

    }
    
}
