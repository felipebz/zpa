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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;
import org.sonar.plsqlopen.checks.PlSqlCheck;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

public class PlSqlChecks {

    private final CheckFactory checkFactory;
    private Set<Checks<PlSqlCheck>> checksByRepository = new HashSet<>();

    private PlSqlChecks(CheckFactory checkFactory) {
      this.checkFactory = checkFactory;
    }

    public static PlSqlChecks createPlSqlCheck(CheckFactory checkFactory) {
      return new PlSqlChecks(checkFactory);
    }

    @SuppressWarnings("rawtypes")
    public PlSqlChecks addChecks(String repositoryKey, Iterable<Class> checkClass) {
      checksByRepository.add(checkFactory
        .<PlSqlCheck>create(repositoryKey)
        .addAnnotatedChecks(checkClass));

      return this;
    }

    public PlSqlChecks addCustomChecks(@Nullable CustomPlSqlRulesDefinition[] customRulesDefinitions) {
      if (customRulesDefinitions != null) {

        for (CustomPlSqlRulesDefinition rulesDefinition : customRulesDefinitions) {
          addChecks(rulesDefinition.repositoryKey(), Lists.newArrayList(rulesDefinition.checkClasses()));
        }
      }

      return this;
    }

    public List<PlSqlCheck> all() {
      List<PlSqlCheck> allVisitors = new ArrayList<>();

      for (Checks<PlSqlCheck> checks : checksByRepository) {
        allVisitors.addAll(checks.all());
      }

      return allVisitors;
    }

    @Nullable
    public RuleKey ruleKey(PlSqlCheck check) {
      RuleKey ruleKey;

      for (Checks<PlSqlCheck> checks : checksByRepository) {
        ruleKey = checks.ruleKey(check);

        if (ruleKey != null) {
          return ruleKey;
        }
      }
      return null;
    }
    
    @VisibleForTesting
    public Set<Checks<PlSqlCheck>> getChecks() {
        return checksByRepository;
    }
    
}
