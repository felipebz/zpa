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
package org.sonar.plsqlopen.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class MethodMatcherTest extends RuleTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void detectSimpleMethodByName() {        
        MethodMatcher matcher = MethodMatcher.create().name("func");
        matches(matcher, "func");
        notMatches(matcher, "foo");
        notMatches(matcher, "foo.func");
    }
    
    @Test
    public void detectAnyMethod() {        
        MethodMatcher matcher = MethodMatcher.create().name(NameCriteria.any());
        matches(matcher, "func");
        matches(matcher, "foo");
    }
    
    @Test
    public void detectAnyMethodInList() {        
        MethodMatcher matcher = MethodMatcher.create().name(NameCriteria.in("func", "foo"));
        matches(matcher, "func");
        matches(matcher, "FOO");
        notMatches(matcher, "bar");
    }
    
    @Test
    public void detectMethodInPackage() {
        MethodMatcher matcher = MethodMatcher.create().packageName("pack").name("func");
        matches(matcher, "pack.func");
        notMatches(matcher, "pkg.func");
        notMatches(matcher, "func");
    }
    
    @Test
    public void detectMethodInAnyPackage() {
        MethodMatcher matcher = MethodMatcher.create().packageName(NameCriteria.any()).name("func");
        matches(matcher, "pack.func");
        matches(matcher, "pkg.func");
    }
    
    @Test
    public void detectMethodInSchema() {
        MethodMatcher matcher = MethodMatcher.create().schema("sch").name("func");
        matches(matcher, "sch.func");
        notMatches(matcher, "bar.func");
        notMatches(matcher, "func");
    }
    
    @Test
    public void detectMethodInAnySchema() {
        MethodMatcher matcher = MethodMatcher.create().schema(NameCriteria.any()).name("func");
        matches(matcher, "sch.func");
        matches(matcher, "bar.func");
    }
    
    @Test
    public void detectMethodInPackageAndSchema() {
        MethodMatcher matcher = MethodMatcher.create().schema("sch").packageName("pack").name("func");
        matches(matcher, "sch.pack.func");
        notMatches(matcher, "bar.pack.func");
    }
    
    @Test
    public void detectMethodInPackageAndOptionalSchema() {
        MethodMatcher matcher = MethodMatcher.create().schema("sch").schemaIsOptional().packageName("pack").name("func");
        matches(matcher, "sch.pack.func");
        matches(matcher, "pack.func");
    }
    
    @Test
    public void detectMethodWithOneParameter() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter();
        matches(matcher, "func(x)");
        notMatches(matcher, "func(x, y)");
        notMatches(matcher, "bar.func(x)");
    }
    
    @Test
    public void detectMethodWithMoreParameters() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameter().addParameter().addParameter();
        matches(matcher, "func(x, y, z)");
        notMatches(matcher, "func(x)");
    }
    
    @Test
    public void detectMethodWithMoreParameters2() {
        MethodMatcher matcher = MethodMatcher.create().name("func").addParameters(3);
        matches(matcher, "func(x, y, z)");
        notMatches(matcher, "func(x)");
    }
    
    @Test
    public void detectMethodInPackageWithParameters() {
        MethodMatcher matcher = MethodMatcher.create().packageName("pack").name("func").addParameter().addParameter().addParameter();
        matches(matcher, "pack.func(x, y, z)");
        notMatches(matcher, "bar.func(x)");
    }
    
    @Test
    public void detectMethodWithoutParameterConstraint() {
        MethodMatcher matcher = MethodMatcher.create().name("func").withNoParameterConstraint();
        matches(matcher, "func(x)");
    }
    
    @Test
    public void detectHostMethodCall() {
        MethodMatcher matcher = MethodMatcher.create().name("func").withNoParameterConstraint();
        matches(matcher, ":func(x)");
    }
    
    @Test
    public void shouldFailIfNameIsCalledMoreThanOnce() {
        MethodMatcher matcher = MethodMatcher.create().name("name");
        exception.expect(IllegalStateException.class);
        matcher.name("name");
    }
    
    @Test
    public void shouldFailIfPackageNameIsCalledMoreThanOnce() {
        MethodMatcher matcher = MethodMatcher.create().packageName("name");
        exception.expect(IllegalStateException.class);
        matcher.packageName("name");
    }
    
    @Test
    public void shouldFailIfSchemaIsCalledMoreThanOnce() {
        MethodMatcher matcher = MethodMatcher.create().schema("name");
        exception.expect(IllegalStateException.class);
        matcher.schema("name");
    }
    
    @Test
    public void shouldFailIfAddParameterIsCalledAfterWithNoParameterConstraint() {
        MethodMatcher matcher = MethodMatcher.create().withNoParameterConstraint();
        exception.expect(IllegalStateException.class);
        matcher.addParameter();
    }
    
    @Test
    public void shouldFailIfAddParametersIsCalledAfterWithNoParameterConstraint() {
        MethodMatcher matcher = MethodMatcher.create().withNoParameterConstraint();
        exception.expect(IllegalStateException.class);
        matcher.addParameters(2);
    }
    
    @Test
    public void shouldFailIfWithNoParameterConstraintIsCalledAfterAddParameter() {
        MethodMatcher matcher = MethodMatcher.create().addParameter();
        exception.expect(IllegalStateException.class);
        matcher.withNoParameterConstraint();
    }
    
    @Test
    public void shouldFailIfWithNoParameterConstraintIsCalledAfterAddParameters() {
        MethodMatcher matcher = MethodMatcher.create().addParameters(2);
        exception.expect(IllegalStateException.class);
        matcher.withNoParameterConstraint();
    }
    
    public void matches(MethodMatcher matcher, String text) {
        assertThat(matcher.matches(p.parse(text).getFirstChild())).isTrue();
    }
    
    public void notMatches(MethodMatcher matcher, String text) {
        assertThat(matcher.matches(p.parse(text).getFirstChild())).isFalse();
    }

}
