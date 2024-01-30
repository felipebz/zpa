/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2024 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
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
package org.sonar.plugins.plsqlopen.api

import org.sonar.api.ExtensionPoint
import org.sonar.api.scanner.ScannerSide
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.plsqlopen.CustomAnnotationBasedRulesDefinition
import org.sonar.plsqlopen.PlSql
import org.sonar.plsqlopen.rules.SonarQubeRepositoryAdapter
import org.sonar.plsqlopen.rules.SonarQubeRuleMetadataLoader

@ExtensionPoint
@ScannerSide
abstract class CustomPlSqlRulesDefinition : RulesDefinition, ZpaRulesDefinition {

    override fun define(context: RulesDefinition.Context) {
        val repo = context.createRepository(repositoryKey(), PlSql.KEY)
                .setName(repositoryName())

        // Load metadata from check classes' annotations
        CustomAnnotationBasedRulesDefinition(SonarQubeRepositoryAdapter(repo), PlSql.KEY, SonarQubeRuleMetadataLoader())
            .addRuleClasses(checkClasses().toList())

        repo.done()
    }

}
