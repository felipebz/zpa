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
