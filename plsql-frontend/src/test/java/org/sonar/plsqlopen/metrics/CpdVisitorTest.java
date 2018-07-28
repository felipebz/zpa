/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2018 Felipe Zorzo
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.duplications.internal.pmd.TokensLine;
import org.sonar.plsqlopen.TestPlSqlVisitorRunner;

import com.google.common.io.Files;

public class CpdVisitorTest {

    private static final String BASE_DIR = "src/test/resources/metrics";
    private static final String FILE = "cpd.sql";
    private SensorContextTester context = SensorContextTester.create(new File(BASE_DIR));

    @Test
    public void scanFile() throws Exception {
        
        DefaultInputFile inputFile = inputFile(FILE);
        context.fileSystem().add(inputFile);
        
        CpdVisitor visitor = new CpdVisitor(context, inputFile);
        TestPlSqlVisitorRunner.scanFile(new File(Paths.get(BASE_DIR, FILE).toString()), null, visitor);
        List<TokensLine> cpdTokenLines = context.cpdTokens(inputFile.key());
        assertThat(cpdTokenLines).hasSize(17);
    }
    
    private DefaultInputFile inputFile(String fileName) throws Exception {
        File file = new File(BASE_DIR, fileName);

        DefaultInputFile inputFile = new TestInputFileBuilder("key", "cpd.sql")
                .setLanguage("plsqlopen")
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(Files.toString(file, StandardCharsets.UTF_8))
                .setModuleBaseDir(Paths.get(BASE_DIR))
                .build();

        context.fileSystem().add(inputFile);

        return inputFile;
    }

}
