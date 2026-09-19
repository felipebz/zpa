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
package com.felipebz.zpa.api.units

import com.felipebz.flr.tests.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest

class CreateViewTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(PlSqlGrammar.CREATE_VIEW)
    }

    @Test
    fun matchesSimpleView() {
        assertThat(p).matches("create view foo as select 1 from dual;")
    }

    @Test
    fun matchesRequiredProductionExamples() {
        assertThat(p).matches(
            "create view v sharing = extended data as select 1 from dual with read only container_map;"
        )
        assertThat(p).matches("create view v (id invisible unique) as select 1 from dual;")
        assertThat(p).matches("create view v of object_type with object id (id) as select 1 from dual;")
        assertThat(p).matches("create view v as select 1 from dual;")
    }

    @Test
    fun matchesSimpleViewWithoutSemicolon() {
        assertThat(p).matches("create view foo as select 1 from dual")
    }

    @Test
    fun matchesCreateOrReplaceView() {
        assertThat(p).matches("create or replace view foo as select 1 from dual;")
    }

    @Test
    fun matchesViewWithSchema() {
        assertThat(p).matches("create view sch.foo as select 1 from dual;")
    }

    @Test
    fun matchesDocumentedOptionOrdering() {
        assertThat(p).matches("create no force editionable view foo as select 1 from dual;")
        assertThat(p).matches("create editioning view foo as select 1 from dual;")
        assertThat(p).matches("create editionable editioning view foo as select 1 from dual;")
        assertThat(p).matches("create noneditionable view foo as select 1 from dual;")
        assertThat(p).matches("create json collection view foo as select 1 from dual;")
    }

    @Test
    fun matchesIfNotExists() {
        assertThat(p).matches("create view if not exists foo as select 1 from dual;")
    }

    @Test
    fun matchesAllSharingValues() {
        listOf("metadata", "data", "extended data", "none").forEach { sharing ->
            assertThat(p).matches("create view foo sharing = $sharing as select 1 from dual;")
        }
    }

    @Test
    fun matchesViewWithColumnVisibility() {
        assertThat(p).matches(
            "create view foo (visible_col visible, invisible_col invisible) as select 1, 2 from dual;"
        )
    }

    @Test
    fun matchesViewWithInlineAndOutOfLineConstraints() {
        assertThat(p).matches(
            "create view foo (id unique, name, constraint pk primary key (id)) as select 1, 2 from dual;"
        )
    }

    @Test
    fun matchesViewWithDefaultCollation() {
        assertThat(p).matches("create view foo default collation binary as select 1 from dual;")
    }

    @Test
    fun matchesViewWithBequeath() {
        assertThat(p).matches("create view foo bequeath current_user as select 1 from dual;")
        assertThat(p).matches("create view foo bequeath definer as select 1 from dual;")
    }

    @Test
    fun matchesStatementAndColumnAnnotations() {
        assertThat(p).matches(
            "create view foo (id invisible annotations (hidden), name annotations (display 'Name')) " +
                "annotations (add if not exists title 'View') as select 1, 2 from dual;"
        )
    }

    @Test
    fun matchesObjectViews() {
        assertThat(p).matches("create view foo of object_type with object id (id) as select 1 from dual;")
        assertThat(p).matches("create view foo of object_type with object identifier default as select 1 from dual;")
        assertThat(p).matches("create view child of object_type under superview (id unique) as select 1 from dual;")
    }

    @Test
    fun matchesXmlTypeViews() {
        assertThat(p).matches(
            "create view foo of xmltype xmlschema 'http://example.test/view.xsd' element 'View' " +
                "with object id (id) as select 1 from dual;"
        )
        assertThat(p).matches(
            "create view foo of xmltype xmlschema 'http://example.test/view.xsd' element " +
                "'http://example.test/other.xsd' # 'View' store all varrays as lobs allow nonschema " +
                "disallow anyschema with object identifier default as select 1 from dual;"
        )
    }

    @Test
    fun matchesContainerEndings() {
        assertThat(p).matches(
            "create view foo as select 1 from dual with read only container_map;"
        )
        assertThat(p).matches(
            "create view foo as select 1 from dual with read only containers_default;"
        )
    }

    @Test
    fun matchesDecodeExpressions() {
        assertThat(p).matches(
            "create or replace view foo as select decode(bp.reference,null,sp.name1,bp.name1) p_name1 from bp, sp;"
        )
        assertThat(p).matches(
            "create or replace view foo as select decode(bp.ref,null,sp.name1,bp.name1) p_name1 from bp, sp;"
        )
    }

    @Test
    fun matchesViewWithRestriction() {
        assertThat(p).matches("create view foo as select 1, 2, 3 from dual with read only;")
        assertThat(p).matches("create view foo as select 1, 2, 3 from dual with check option;")
        assertThat(p).matches(
            "create view foo as select 1, 2, 3 from dual with check option constraint cons_name;"
        )
    }

    @Test
    fun matchesViewWithOrder() {
        assertThat(p).matches("create or replace view foo as (select abc,1 from dual) order by abc;")
    }

    @Test
    fun rejectsMaterializedViewSyntax() {
        assertThat(p).notMatches("create materialized view foo as select 1 from dual;")
        assertThat(p).notMatches("create materialized view v as select 1 from dual;")
        assertThat(p).notMatches(
            "create materialized view foo pctfree 0 tablespace dat3 refresh complete as select 1 from dual;"
        )
    }

    @Test
    fun rejectsNonOracleOptionOrder() {
        assertThat(p).notMatches("create editionable force view foo as select 1 from dual;")
    }

    @Test
    fun rejectsConflictingViewClauses() {
        assertThat(p).notMatches(
            "create view foo (id) of object_type with object id (id) as select 1 from dual;"
        )
        assertThat(p).notMatches(
            "create view foo of object_type with object id (id) of xmltype with object id default " +
                "as select 1 from dual;"
        )
    }

    @Test
    fun rejectsIncompleteOptions() {
        assertThat(p).notMatches("create view foo sharing = as select 1 from dual;")
        assertThat(p).notMatches("create view foo bequeath as select 1 from dual;")
    }

    @Test
    fun rejectsMalformedAnnotationsAndConstraints() {
        assertThat(p).notMatches("create view foo annotations () as select 1 from dual;")
        assertThat(p).notMatches("create view foo annotations (add if exists title) as select 1 from dual;")
        assertThat(p).notMatches("create view foo (id annotations (hidden) as select 1 from dual;")
        assertThat(p).notMatches("create view foo (id unique as select 1 from dual;")
    }
}
