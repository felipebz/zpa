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
package org.sonar.plugins.plsqlopen.api.expressions;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class MultisetExpressionsTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.EXPRESSION);
    }
    
    @Test
    public void matchesIsASet() {
        assertThat(p).matches("foo is a set");
    }
    
    @Test
    public void matchesIsNotASet() {
        assertThat(p).matches("foo is not a set");
    }
    
    @Test
    public void matchesIsEmpty() {
        assertThat(p).matches("foo is not a set");
    }
    
    @Test
    public void matchesIsNotEmpty() {
        assertThat(p).matches("foo is not a set");
    }
    
    @Test
    public void matchesMemberExpression() {
        assertThat(p).matches("foo member bar");
    }
    
    @Test
    public void matchesMemberOfExpression() {
        assertThat(p).matches("foo member of bar");
    }
    
    @Test
    public void matchesNotMemberOfExpression() {
        assertThat(p).matches("foo not member of bar");
    }
    
    @Test
    public void matchesSimpleSubmultiset() {
        assertThat(p).matches("foo submultiset bar");
    }
    
    @Test
    public void matchesSubmultisetOf() {
        assertThat(p).matches("foo submultiset of bar");
    }
    
    @Test
    public void matchesNotSubmultiset() {
        assertThat(p).matches("foo not submultiset bar");
    }
    
    @Test
    public void matchesNotSubmultisetOf() {
        assertThat(p).matches("foo not submultiset of bar");
    }
    
    @Test
    public void matchesMultisetExcept() {
        assertThat(p).matches("foo multiset except bar");
    }
    
    @Test
    public void matchesMultisetExceptAll() {
        assertThat(p).matches("foo multiset except all bar");
    }
    
    @Test
    public void matchesMultisetExceptDistinct() {
        assertThat(p).matches("foo multiset except distinct bar");
    }
    
    @Test
    public void doesNotMatcheMultisetExceptAllDistinct() {
        assertThat(p).notMatches("foo multiset except all distinct");
    }
    
    @Test
    public void matchesMultisetIntersect() {
        assertThat(p).matches("foo multiset intersect bar");
    }
    
    @Test
    public void matchesMultisetIntersectWithFunctions() {
        assertThat(p).matches("foo(1,2,3) multiset intersect bar('a', 'b', 'c')");
    }
    
    @Test
    public void matchesMultisetUnion() {
        assertThat(p).matches("foo multiset union bar");
    }
    
    @Test
    public void matchesMultipleMultisetUnion() {
        assertThat(p).matches("a multiset union b multiset union c multiset union d");
    }
}
