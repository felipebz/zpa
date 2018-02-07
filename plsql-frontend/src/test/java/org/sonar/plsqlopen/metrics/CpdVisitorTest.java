package org.sonar.plsqlopen.metrics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.duplications.internal.pmd.TokensLine;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

public class CpdVisitorTest {

    private SensorContextTester context;
    private DefaultInputFile inputFile;
    private String key;

    @Before
    public void scanFile() throws IOException {
        File dir = new File("src/test/resources/metrics");

        File file = new File(dir, "/cpd.sql");
        inputFile = new TestInputFileBuilder("key", "cpd.sql")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(Files.toString(file, StandardCharsets.UTF_8))
                .setModuleBaseDir(dir.toPath())
                .build();
        key = inputFile.key();

        context = SensorContextTester.create(dir);
        context.fileSystem().add(inputFile);
        
        PlSqlAstScanner scanner = new PlSqlAstScanner(context, ImmutableList.of(), new NoSonarFilter(), null, false);
        scanner.scanFile(inputFile);
    }

    @Test
    public void testCpdTokens() { 
        List<TokensLine> cpdTokenLines = context.cpdTokens(key);
        assertThat(cpdTokenLines).hasSize(17);
    }

}
