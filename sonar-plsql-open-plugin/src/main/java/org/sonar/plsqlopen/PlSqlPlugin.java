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

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class PlSqlPlugin implements Plugin {

    public static final String FILE_SUFFIXES_KEY = "sonar.plsql.file.suffixes";
    public static final String FORMS_METADATA_KEY = "sonar.plsql.forms.metadata";
    public static final String ERROR_RECOVERY_KEY = "sonar.plsql.errorRecoveryEnabled";

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
            PropertyDefinition.builder(FORMS_METADATA_KEY)
                .name("Oracle Forms metadata file")
                .description("Path to the JSON file with the Oracle Forms metadata.")
                .category("PL/SQL")
                .onQualifiers(Qualifiers.PROJECT)
                .build(),
            PropertyDefinition.builder(ERROR_RECOVERY_KEY)
                .name("Parse error recovery")
                .description("Defines mode for error handling of parsing errors. 'False' (strict) breaks after an error or 'True' (tolerant, default) continues.")
                .category("PL/SQL")
                .onQualifiers(Qualifiers.PROJECT)
                .type(PropertyType.BOOLEAN)
                .defaultValue("true")
                .build(),
          
            PlSql.class,
            PlSqlProfile.class,
            PlSqlSquidSensor.class,
            PlSqlRuleRepository.class);        
    }

}
