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
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;

import br.com.felipezorzo.sonar.plsql.PlSql;
import br.com.felipezorzo.sonar.plsql.PlSqlCpdMapping;
import br.com.felipezorzo.sonar.plsql.PlSqlTokenizer;

public class PlSqlCpdMappingTest {

    @Test
    public void test() {
        PlSql language = mock(PlSql.class);
        FileSystem fs = mock(FileSystem.class);
        PlSqlCpdMapping mapping = new PlSqlCpdMapping(language, fs);
        assertThat(mapping.getLanguage()).isSameAs(language);
        assertThat(mapping.getTokenizer()).isInstanceOf(PlSqlTokenizer.class);
    }

}
