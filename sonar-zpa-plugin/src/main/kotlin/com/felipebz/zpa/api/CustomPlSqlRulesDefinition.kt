//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api

import com.felipebz.zpa.CustomAnnotationBasedRulesDefinition
import com.felipebz.zpa.PlSql
import com.felipebz.zpa.rules.SonarQubeRepositoryAdapter
import com.felipebz.zpa.rules.SonarQubeRuleMetadataLoader
import org.sonar.api.ExtensionPoint
import org.sonar.api.scanner.ScannerSide
import org.sonar.api.server.rule.RulesDefinition

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
