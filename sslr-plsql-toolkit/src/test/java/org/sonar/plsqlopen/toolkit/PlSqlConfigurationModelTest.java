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
package org.sonar.plsqlopen.toolkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.plsqlopen.toolkit.PlSqlConfigurationModel;

public class PlSqlConfigurationModelTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void getConfiguration_charset() {
        PlSqlConfigurationModel model = new PlSqlConfigurationModel();
        model.charsetProperty.setValue("UTF-8");
        assertThat(model.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(model.getConfiguration().getCharset()).isEqualTo(StandardCharsets.UTF_8);
        model.charsetProperty.setValue("ISO-8859-1");
        assertThat(model.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
        assertThat(model.getConfiguration().getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void getPropertyOrDefaultValue_with_property_set() {
        String oldValue = System.getProperty("foo");

        try {
            System.setProperty("foo", "bar");
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("bar");
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo");
            } else {
                System.setProperty("foo", oldValue);
            }
        }
    }

    @Test
    public void getPropertyOrDefaultValue_with_property_not_set() {
        String oldValue = System.getProperty("foo");

        try {
            System.clearProperty("foo");
            assertThat(PlSqlConfigurationModel.getPropertyOrDefaultValue("foo", "baz")).isEqualTo("baz");
        } finally {
            if (oldValue == null) {
                System.clearProperty("foo");
            } else {
                System.setProperty("foo", oldValue);
            }
        }
    }
}
