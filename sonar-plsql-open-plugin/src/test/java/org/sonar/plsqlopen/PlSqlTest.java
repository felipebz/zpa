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

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.plsqlopen.PlSqlPlugin;

public class PlSqlTest {
    
    @Test
    public void test() {
        PlSql language = new PlSql(new MapSettings());
        assertThat(language.getKey()).isEqualTo("plsqlopen");
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
        MapSettings settings = new MapSettings();
        settings.setProperty(PlSqlPlugin.FILE_SUFFIXES_KEY, "sql, custom");

        PlSql language = new PlSql(settings);
        assertThat(language.getFileSuffixes()).hasSize(2).contains("custom");
    }
    
}
