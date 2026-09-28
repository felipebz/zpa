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
import org.junit.jupiter.api.Test

class ConstraintAttributePathTest : RuleTest() {

    @Test
    fun matchesAttributePathsInObjectTables() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches(
            "create table persons of person ( homeaddress not null, unique (homeaddress.phone), " +
                "check (homeaddress.zip is not null), check (homeaddress.city <> 'San Francisco') );")
        assertThat(p).matches("create table t of person (primary key (homeaddress.phone))")
        assertThat(p).matches("create table t of person (unique (homeaddress.geo.lat))")
        assertThat(p).matches("create table t of person (unique (name, homeaddress.phone))")
        assertThat(p).matches("create table t of person (unique (\"HOMEADDRESS\".\"PHONE\"))")
        // Unknown attributes and extra qualifiers are resolved later (ORA-22809/ORA-00904).
        assertThat(p).matches("create table t of person (unique (homeaddress.nx))")
        assertThat(p).matches("create table t of person (unique (t.homeaddress.phone))")
    }

    @Test
    fun matchesAttributePathsInRelationalTables() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table t (id number, addr address_t, unique (addr.phone))")
        assertThat(p).matches("create table t (id number, addr address_t, primary key (addr.geo.lat))")
        assertThat(p).matches("create table t (id number, addr address_t, constraint u1 unique (addr.phone) disable)")
        // Type compatibility and object-type checks happen later (ORA-02267/ORA-02337).
        assertThat(p).matches("create table t (id number, addr address_t, foreign key (addr.zip) references p (id))")
        assertThat(p).matches("create table t (id number, foreign key (id) references p (a.b))")
        assertThat(p).matches("create table t (id number, c number references p (a.b))")
    }

    @Test
    fun matchesAttributePathsInAlterTable() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter table t add unique (addr.phone);")
        assertThat(p).matches("alter table t add constraint u1 primary key (addr.phone);")
        assertThat(p).matches("alter table t add foreign key (addr.zip) references p (id);")
    }

    @Test
    fun rejectsMalformedPaths() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("create table t of person (unique (homeaddress.))")
        assertThat(p).notMatches("create table t of person (unique (.phone))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress..phone))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone garbage))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone,))")
        assertThat(p).notMatches("create table t (id number, addr address_t, unique (addr.phone.))")
        assertThat(p).notMatches("create table t (id number, foreign key (addr.) references p (id))")
        assertThat(p).notMatches("create table t (id number, foreign key (id) references p (a.))")
    }

    @Test
    fun keepsPlainColumnsInViewConstraints() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("alter view v add constraint pk primary key (id) rely disable novalidate;")
        assertThat(p).notMatches("alter view v add constraint pk primary key (addr.phone) rely disable novalidate;")
    }

    @Test
    fun matchesDatabaseLinksOnConstraintColumns() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        // Oracle 26 accepts a database link on each constraint column and ignores it.
        assertThat(p).matches("create table t of person (unique (homeaddress.phone@dbl));")
        assertThat(p).matches("create table t of person (primary key (name@dbl));")
        assertThat(p).matches("create table t of person (unique (homeaddress.geo.lat@dbl, name@dbl));")
        assertThat(p).matches("create table t of person (unique (\"HOMEADDRESS\".\"PHONE\"@\"Dbl\"));")
        assertThat(p).matches("create table t of person (unique (homeaddress.phone@dbl.x.y));")
        // The link swallows the following dotted names: this is column homeaddress at dbl.phone (ORA-02329 later).
        assertThat(p).matches("create table t of person (unique (homeaddress@dbl.phone));")
        assertThat(p).matches("create table t (id number primary key, c number, foreign key (c@dbl) references t (id));")
        assertThat(p).matches("create table t (id number, c number, foreign key (c) references p (id@dbl));")
        assertThat(p).matches("create table t (id number, c number references p (id@dbl));")
        assertThat(p).matches("alter table t add unique (addr.phone@dbl);")
        assertThat(p).matches("alter table t add foreign key (id@dbl) references p (id);")
    }

    @Test
    fun rejectsMalformedDatabaseLinks() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone@))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone@1))")
        assertThat(p).notMatches("create table t of person (unique (@dbl))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone@dbl garbage))")
        assertThat(p).notMatches("create table t of person (unique (homeaddress.phone@dbl.))")
        // Only one connection qualifier, undotted and non-empty (ORA-02083/ORA-02084).
        assertThat(p).notMatches("create table t of person (unique (name@db@))")
        assertThat(p).notMatches("create table t of person (unique (name@db@@q))")
        assertThat(p).notMatches("create table t of person (unique (name@db@q@x))")
        assertThat(p).notMatches("create table t of person (unique (name@db@q.x))")
        assertThat(p).notMatches("create table t of person (unique (name@@q@x))")
        assertThat(p).notMatches("create table t of person (unique (name@@q.x))")
        assertThat(p).notMatches("create table t of person (unique (name@@))")
        assertThat(p).notMatches("create table t of person (unique (name@@1))")
        assertThat(p).notMatches("create table t of person (unique (name@db@q garbage))")
    }

    @Test
    fun matchesConnectionQualifiers() {
        setRootRule(DdlGrammar.DDL_COMMAND)
        assertThat(p).matches("create table t of person (unique (name@db@q));")
        assertThat(p).matches("create table t of person (unique (homeaddress.geo.lat@db.dom@q));")
        assertThat(p).matches("create table t of person (primary key (homeaddress.phone@db@q));")
        assertThat(p).matches("create table t of person (unique (name@\"Db\"@\"Q\"));")
        // The database name may be omitted, leaving only the qualifier.
        assertThat(p).matches("create table t of person (unique (name@@q, homeaddress.phone@@q));")
        assertThat(p).matches("create table t (id number primary key, c number, foreign key (c@db@q) references t (id));")
        assertThat(p).matches("create table t (id number primary key, c number, foreign key (c@@q) references t (id));")
        assertThat(p).matches("create table t (id number, c number, foreign key (c) references p (id@db.dom@q));")
        assertThat(p).matches("create table t (id number, c number references p (id@@q));")
        assertThat(p).matches("alter table t add unique (addr.phone@db@q);")
        assertThat(p).matches("alter table t add unique (addr.phone@@q);")
    }
}
