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
package org.sonar.plugins.plsqlopen.api.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;

import com.sonar.sslr.api.AstNode;

public class MethodMatcherWithTypesTest extends RuleTest {


    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }

    @Test
    public void matchesMethodWhenTheTypeIsNotSpecifiedInTheMatcher() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter();
        SemanticAstNode node = getAstNodeWithArguments("func(x)", PlSqlType.NUMERIC);
        assertThat(matcher.matches(node)).isTrue();
    }

    @Test
    public void matchesMethodWhenTheTypeIsSpecifiedAsUnknownInTheMatcher() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.UNKNOWN);
        SemanticAstNode node = getAstNodeWithArguments("func(x)", PlSqlType.NUMERIC);
        assertThat(matcher.matches(node)).isTrue();
    }

    @Test
    public void matchesMethodWhenTheTypeIsSpecifiedInTheMatcher() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.NUMERIC);
        SemanticAstNode node = getAstNodeWithArguments("func(x)", PlSqlType.NUMERIC);
        assertThat(matcher.matches(node)).isTrue();
    }

    @Test
    public void notMatchesMethodWhenTheTypeIsDifferentFromExpectation() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter(PlSqlType.CHARACTER);
        SemanticAstNode node = getAstNodeWithArguments("func(x)", PlSqlType.NUMERIC);
        assertThat(matcher.matches(node)).isFalse();
    }

    @Test
    public void matchesMethodWithMultipleParametersWhenTheTypeIsSpecifiedInTheMatcher() {
        MethodMatcher matcher = MethodMatcher.create().name("func")
            .addParameters(PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.DATE);
        SemanticAstNode node = getAstNodeWithArguments("func(x, y, z)", PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.DATE);
        assertThat(matcher.matches(node)).isTrue();
    }

    @Test
    public void noMatchesMethodWithMultipleParametersWhenTheAnyTypeIsDifferentFromExpectation() {
        MethodMatcher matcher = MethodMatcher.create().name("func")
            .addParameters(PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.DATE);
        SemanticAstNode node = getAstNodeWithArguments("func(x, y, z)", PlSqlType.NUMERIC, PlSqlType.CHARACTER, PlSqlType.CHARACTER);
        assertThat(matcher.matches(node)).isFalse();
    }

    private SemanticAstNode getAstNodeWithArguments(String text, PlSqlType... types) {
        SemanticAstNode node = PlSqlAstScanner.getSemanticNode(p.parse(text).getFirstChild());

        List<AstNode> arguments = node.getDescendants(PlSqlGrammar.ARGUMENT);

        for (int i = 0; i < types.length; i++) {
            AstNode actualArgument = arguments.get(i).getFirstChild();
            MethodMatcher.semantic(actualArgument).setPlSqlType(types[i]);
        }

        return node;
    }
}
