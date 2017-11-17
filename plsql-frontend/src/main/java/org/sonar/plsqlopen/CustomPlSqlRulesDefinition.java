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

import com.google.common.collect.ImmutableList;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.server.rule.RulesDefinition;

@ExtensionPoint
@BatchSide
public abstract class CustomPlSqlRulesDefinition implements RulesDefinition {

    @Override
    public void define(RulesDefinition.Context context) {
        RulesDefinition.NewRepository repo = context.createRepository(repositoryKey(), "plsqlopen")
                .setName(repositoryName());

        // Load metadata from check classes' annotations
        new CustomAnnotationBasedRulesDefinition(repo, "plsqlopen").addRuleClasses(false,
                ImmutableList.copyOf(checkClasses()));

        repo.done();
    }

    public abstract String repositoryName();

    public abstract String repositoryKey();

    @SuppressWarnings("rawtypes")
    public abstract Class[] checkClasses();
    
}
