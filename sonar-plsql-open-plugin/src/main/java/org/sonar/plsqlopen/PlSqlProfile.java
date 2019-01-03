/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
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

import org.sonar.api.rules.RuleAnnotationUtils;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.plsqlopen.annnotations.ActivatedByDefault;
import org.sonar.plsqlopen.checks.CheckList;

public class PlSqlProfile implements BuiltInQualityProfilesDefinition {

    @Override
    public void define(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(CheckList.SONAR_WAY_PROFILE, PlSql.KEY);
        profile.setDefault(true);
        
        for (Class<?> ruleClass : CheckList.getChecks()) {
            addRule(ruleClass, profile, CheckList.REPOSITORY_KEY);
        }
        
        profile.done();
    }
    
    private void addRule(Class<?> ruleClass, NewBuiltInQualityProfile profile, String repositoryKey) {
        if (AnnotationUtils.getAnnotation(ruleClass, ActivatedByDefault.class) != null) {
            String ruleKey = RuleAnnotationUtils.getRuleKey(ruleClass);
            profile.activateRule(repositoryKey, ruleKey);
        }
    }

}
