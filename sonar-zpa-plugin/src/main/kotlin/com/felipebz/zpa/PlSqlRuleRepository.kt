/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2025 Felipe Zorzo
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
package com.felipebz.zpa

import com.felipebz.zpa.checks.CheckList
import com.felipebz.zpa.rules.SonarQubeRepositoryAdapter
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import org.sonar.api.server.rule.RulesDefinition

class PlSqlRuleRepository : RulesDefinition {

    override fun define(context: RulesDefinition.Context) {
        val repository = context
                .createRepository(KEY, PlSql.Companion.KEY)
                .setName("ZPA")
        CustomAnnotationBasedRulesDefinition.load(SonarQubeRepositoryAdapter(repository), PlSql.Companion.KEY, CheckList.checks, SonarQubeRuleMetadataLoader())
        repository.done()
    }

    companion object {
        internal val KEY: String

        init {
            // TODO: remove this code and always use the key "zpa"
            var repositoryKey = "plsql"
            if (SonarQubeUtils.isCommercialEdition) {
                repositoryKey = "zpa"
            }
            KEY = repositoryKey
        }
    }

}
