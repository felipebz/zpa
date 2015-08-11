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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.sonar.api.config.Settings;

import com.google.common.collect.Maps;

public class PlSqlTest {
    
    @Test
    public void test() {
        PlSql language = new PlSql(new Settings());
        assertThat(language.getKey()).isEqualTo("plsql");
        assertThat(language.getName()).isEqualTo("PL/SQL");
        assertThat(language.getFileSuffixes())
            .hasSize(4)
            .contains("sql")
            .contains("pkg")
            .contains("pks")
            .contains("pkb");
    }
    
    @Test
    public void custom_file_suffixes() {
        Map<String, String> props = Maps.newHashMap();
        props.put(PlSqlPlugin.FILE_SUFFIXES_KEY, "sql, custom");

        Settings settings = new Settings();
        settings.addProperties(props);

        PlSql language = new PlSql(settings);
        assertThat(language.getFileSuffixes()).hasSize(2).contains("custom");
    }
    
}
