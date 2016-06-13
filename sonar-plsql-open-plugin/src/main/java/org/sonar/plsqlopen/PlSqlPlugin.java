/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2016 Felipe Zorzo
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

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class PlSqlPlugin implements Plugin {

    public static final String FILE_SUFFIXES_KEY = "sonar.plsql.file.suffixes";

    @Override
    public void define(Context context) {
        context.addExtensions(
            PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                .name("File Suffixes")
                .description("Comma-separated list of suffixes of PL/SQL files to analyze.")
                .category("PL/SQL")
                .onQualifiers(Qualifiers.PROJECT)
                .defaultValue("sql,pkg,pks,pkb")
                .build(),
          
        PlSql.class,
        PlSqlProfile.class,
        PlSqlSquidSensor.class,
        PlSqlRuleRepository.class,
        SonarComponents.class);        
    }

}
