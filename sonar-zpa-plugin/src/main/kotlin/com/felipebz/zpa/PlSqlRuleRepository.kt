//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

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
