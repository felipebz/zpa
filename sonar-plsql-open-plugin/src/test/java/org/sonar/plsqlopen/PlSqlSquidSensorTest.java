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
package org.sonar.plsqlopen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.metadata.FormsMetadata;

import com.google.common.io.Files;

public class PlSqlSquidSensorTest {

    private PlSqlSquidSensor sensor;
    private SensorContextTester context;
    
    @Before
    public void setUp() {
        ActiveRules activeRules = (new ActiveRulesBuilder())
                .create(RuleKey.of(CheckList.REPOSITORY_KEY, "EmptyBlock"))
                .setName("Print Statement Usage")
                .activate()
                .build();
        CheckFactory checkFactory = new CheckFactory(activeRules);
        context = SensorContextTester.create(new File("."));
        sensor = new PlSqlSquidSensor(checkFactory, new MapSettings(), new NoSonarFilter());
    }
    
    @Test
    public void testDescriptor() {
        DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
        sensor.describe(descriptor);
        assertThat(descriptor.name()).isEqualTo("PlsqlSquidSensor");
        assertThat(descriptor.languages()).containsOnly(PlSql.KEY);
    }
    
    @Test
    public void shouldAnalyse() throws IOException {
      String relativePath = "src/test/resources/org/sonar/plsqlopen/code.sql";
      DefaultInputFile inputFile = new TestInputFileBuilder("key", relativePath)
              .setLanguage(PlSql.KEY)
              .setCharset(StandardCharsets.UTF_8)
              .initMetadata(Files.toString(new File(relativePath), StandardCharsets.UTF_8))
              .setModuleBaseDir(Paths.get(""))
              .build();
      
      context.fileSystem().add(inputFile);
      
      sensor.execute(context);
      
      String key = "key:" + relativePath;

      //assertThat(context.measure(key, CoreMetrics.FILES).value()).isEqualTo(1);
      assertThat(context.measure(key, CoreMetrics.NCLOC).value()).isEqualTo(18);
      assertThat(context.measure(key, CoreMetrics.COMMENT_LINES).value()).isEqualTo(2);
      assertThat(context.measure(key, CoreMetrics.COMPLEXITY).value()).isEqualTo(6);
      assertThat(context.measure(key, CoreMetrics.FUNCTIONS).value()).isEqualTo(2);
      assertThat(context.measure(key, CoreMetrics.STATEMENTS).value()).isEqualTo(8);

    }
    
    @Test
    public void canReadSimpleMetadaFile() {
        sensor.loadMetadataFile("src/test/resources/metadata/metadata.json");
        FormsMetadata metadata = sensor.getFormsMetadata();
        
        assertThat(metadata.getAlerts()).containsExactly("foo", "bar");
        assertThat(metadata.getBlocks()).hasSize(2);
        assertThat(metadata.getBlocks()[0].getName()).isEqualTo("foo");
        assertThat(metadata.getBlocks()[0].getItems()).containsExactly("item1", "item2");
        assertThat(metadata.getBlocks()[1].getName()).isEqualTo("bar");
        assertThat(metadata.getBlocks()[1].getItems()).containsExactly("item1", "item2");
        assertThat(metadata.getLovs()).containsExactly("foo", "bar");
    }
    
}
