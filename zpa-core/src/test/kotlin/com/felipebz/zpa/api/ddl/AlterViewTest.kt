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
package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlterViewTest : RuleTest() {
    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_VIEW)
    }

    @Test
    fun matchesRecompilationAndEditionActions() {
        assertThat(p).matches("alter view customer_ro compile;")
        assertThat(p).matches("alter view customer_ro recompile")
        assertThat(p).matches("alter view customer_ro editionable;")
        assertThat(p).matches("alter view customer_ro noneditionable;")
        assertThat(p).matches("alter view if exists app.customer_ro compile;")
        assertThat(p).matches("alter view app.customer_ro read only;")
        assertThat(p).matches("alter view app.customer_ro read write;")
    }

    @Test
    fun matchesViewAndColumnAnnotations() {
        assertThat(p).matches("alter view HighWageEmp annotations(drop Title, add Identity);")
        assertThat(p).matches("alter view v annotations(add or replace Title 'Title', drop if exists Old)")
        assertThat(p).matches("alter view v modify (id annotations(Label 'ID'))")
        assertThat(p).matches("alter view v modify " +
            "(id annotations(add Label), name annotations(drop Old, add Title 'Name'));")
    }

    @Test
    fun matchesViewConstraintActions() {
        assertThat(p).matches("alter view v add constraint pk primary key (id) rely disable novalidate")
        assertThat(p).matches("alter view v add (constraint pk primary key (id) disable novalidate, " +
            "constraint uk unique (name) disable novalidate)")
        assertThat(p).matches("alter view v add foreign key (id) references parent(id) disable novalidate")
        assertThat(p).matches("alter view v modify constraint pk rely")
        assertThat(p).matches("alter view v modify constraint pk norely")
        assertThat(p).matches("alter view v modify primary key rely")
        assertThat(p).matches("alter view v add constraint uk unique (name) disable")
        assertThat(p).matches("alter view v drop constraint pk")
        assertThat(p).matches("alter view v drop primary key")
        assertThat(p).matches("alter view v drop unique (id, name)")
    }

    @Test
    fun matchesMultipleActionsAcceptedByOracle26() {
        assertThat(p).matches("alter view v compile editionable")
        assertThat(p).matches("alter view v compile compile")
        assertThat(p).matches("alter view v editionable noneditionable")
        assertThat(p).matches("alter view v noneditionable editionable")
        assertThat(p).matches("alter view v annotations(add A) annotations(add B)")
        assertThat(p).matches("alter view v compile recompile compile")
        assertThat(p).matches("alter view v modify (id annotations(add A)) " +
            "modify (name annotations(add B))")
        assertThat(p).matches("alter view v editionable compile")
        assertThat(p).matches("alter view v compile annotations(add Title) " +
            "modify (id annotations(add Label))")
        assertThat(p).matches("alter view v add constraint pk primary key (id) disable novalidate compile")
        assertThat(p).matches("alter view v add constraint pk primary key (id) disable novalidate " +
            "add constraint uk unique (name) disable novalidate")
        assertThat(p).matches("alter view v drop constraint pk compile")
        assertThat(p).matches("alter view v compile modify constraint pk norely")
    }

    @Test
    fun rejectsMalformedHeadersAndActions() {
        assertThat(p).notMatches("alter view v")
        assertThat(p).notMatches("alter view if not exists v compile")
        assertThat(p).notMatches("alter view a.b.c compile")
        assertThat(p).notMatches("alter view v with read only")
        assertThat(p).notMatches("alter view v read")
        assertThat(p).notMatches("alter view v edit")
        assertThat(p).notMatches("alter view v compile unexpected")
    }

    @Test
    fun rejectsMalformedAnnotationsAndConstraintBoundaries() {
        assertThat(p).notMatches("alter view v annotations()")
        assertThat(p).notMatches("alter view v modify id annotations(add Label)")
        assertThat(p).notMatches("alter view v modify ()")
        assertThat(p).notMatches("alter view v modify (id)")
        assertThat(p).notMatches("alter view v modify (id annotations(add Label),)")
        assertThat(p).notMatches("alter view v modify (id annotations(add Label) " +
            "name annotations(add Title))")
        assertThat(p).notMatches("alter view v modify constraint pk")
        assertThat(p).notMatches("alter view v modify constraint pk disable")
        assertThat(p).notMatches("alter view v modify unique (id) norely")
        assertThat(p).notMatches("alter view v add ()")
        assertThat(p).notMatches("alter view v add constraint ck check (id > 0) disable novalidate")
        assertThat(p).notMatches("alter view v add (constraint ck check (id > 0) disable novalidate)")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id)")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) enable")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) enable novalidate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) disable validate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) deferrable disable novalidate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) initially deferred disable novalidate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) using index disable novalidate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) " +
            "disable novalidate exceptions into errors")
        assertThat(p).notMatches("alter view v add constraint fk foreign key (id) references parent(id) " +
            "on delete cascade disable novalidate")
        assertThat(p).notMatches("alter view v add constraint fk foreign key (id) references parent(id) " +
            "on delete set null disable novalidate")
        assertThat(p).notMatches("alter view v add constraint pk primary key (id) disable novalidate, " +
            "constraint uk unique (name) disable novalidate")
        assertThat(p).notMatches("alter view v add (constraint pk primary key (id),)")
        assertThat(p).notMatches("alter view v drop unique ()")
        assertThat(p).notMatches("alter view v drop unique id")
        assertThat(p).notMatches("alter view v drop constraint pk cascade")
    }

    @Test
    fun routesThroughDdlCommand() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter view customer_ro compile;")
        assertThat(p).matches("alter view HighWageEmp annotations(drop Title, add Identity);")
        assertThat(p).matches("alter view customer_ro editionable;")
    }
}
