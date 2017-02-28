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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.plsqlopen.checks.CheckList;

public class PlSqlProfileTest {

    @Test
    public void should_create_sonar_way_profile() {
        ValidationMessages validation = ValidationMessages.create();

        RuleFinder ruleFinder = ruleFinder();
        PlSqlProfile definition = new PlSqlProfile(ruleFinder);
        RulesProfile profile = definition.createProfile(validation);

        assertThat(profile.getLanguage()).isEqualTo(PlSql.KEY);
        assertThat(profile.getName()).isEqualTo(CheckList.SONAR_WAY_PROFILE);
        assertThat(profile.getActiveRulesByRepository(CheckList.REPOSITORY_KEY)).isNotEmpty();
        assertThat(validation.hasErrors()).isFalse();
    }

    static RuleFinder ruleFinder() {
        return when(mock(RuleFinder.class).findByKey(anyString(), anyString())).thenAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            return Rule.create((String) arguments[0], (String) arguments[1], (String) arguments[1]);
        }).getMock();
    }
}
