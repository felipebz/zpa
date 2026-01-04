/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
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
package com.felipebz.zpa.api.expressions

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class JsonTransformExpressionTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.EXPRESSION)
    }

    @Test
    fun matchesJsonTransformAppend() {
        assertThat(p).matches("json_transform(foo, append 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformCase() {
        assertThat(p).matches("json_transform(foo, case when 'x' then (append 'a' = 'b') end)")
    }

    @Test
    fun matchesJsonTransformCopy() {
        assertThat(p).matches("json_transform(foo, copy 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformInsert() {
        assertThat(p).matches("json_transform(foo, insert 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformIntersect() {
        assertThat(p).matches("json_transform(foo, intersect 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformKeep() {
        assertThat(p).matches("json_transform(foo, keep 'a')")
    }

    @Test
    fun matchesJsonTransformMerge() {
        assertThat(p).matches("json_transform(foo, merge 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformMinus() {
        assertThat(p).matches("json_transform(foo, minus 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformNested() {
        assertThat(p).matches("json_transform(foo, nested path 'x' (copy 'a' = 'b'))")
    }

    @Test
    fun matchesJsonTransformPrepend() {
        assertThat(p).matches("json_transform(foo, prepend 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformRemove() {
        assertThat(p).matches("json_transform(foo, remove 'a')")
    }

    @Test
    fun matchesJsonTransformRename() {
        assertThat(p).matches("json_transform(foo, rename 'a' with 'b')")
    }

    @Test
    fun matchesJsonTransformReplace() {
        assertThat(p).matches("json_transform(foo, replace 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformSet() {
        assertThat(p).matches("json_transform(foo, set 'a' = 'b')")
    }

    @Test
    fun matchesJsonTransformSort() {
        assertThat(p).matches("json_transform(foo, sort 'a')")
    }

    @Test
    fun matchesJsonTransformUnion() {
        assertThat(p).matches("json_transform(foo, union 'a' = 'b')")
    }

    @Test
    fun matchesLongJsonTransform() {
        assertThat(p).matches("""json_transform(foo, 
            append 'a' = 'b' ignore on missing ignore on mismatch ignore on null ignore on empty,
            case when 'x' then (
              append 'a' = 'b',
              append 'c' = 'd'
            ) else (
              append 'e' = 'f',
              append 'g' = 'h'
            ) end,
            copy 'a' = 'b' ignore on missing ignore on null ignore on empty,
            insert 'a' = 'b' ignore on existing ignore on null ignore on empty ignore on error,
            intersect 'a' = 'b' ignore on missing ignore on null,
            keep 'a', 'b', 'c' ignore on missing,
            merge 'a' = 'b' ignore on missing ignore on mismatch ignore on null ignore on empty,
            minus 'a' = 'b' ignore on missing ignore on null,
            nested path 'x' (
              copy 'a' = 'b',
              insert 'c' = 'd'
            ),
            prepend 'a' = 'b' ignore on missing ignore on mismatch ignore on null ignore on empty,
            remove 'a' ignore on missing,
            rename 'a' with 'b' ignore on missing,
            replace 'a' = 'b' ignore on missing ignore on null ignore on empty ignore on error,
            set 'a' = 'b' ignore on existing ignore on missing ignore on null ignore on empty ignore on error,
            sort 'a' reverse,
            sort 'a' remove nulls order by 'b' desc, 'c' asc,
            sort 'a' desc unique remove nulls ignore on missing ignore on mismatch ignore on empty ignore on error,
            union 'a' = 'b' ignore on missing ignore on null)""")
    }

}
