//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test

class XmlTypeStorageTest : RuleTest() {

    @Test
    fun matchesXmlTypeTable() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table t of xmltype xmltype store as clob;")
        assertThat(p).matches("create table t of xmltype xmltype store as transportable binary xml;")
        assertThat(p).matches("create table t of xmltype xmltype store as not transportable binary xml")
        assertThat(p).matches("create table t of xmltype xmlschema \"http://x/w.xsd\" element \"Warehouse\";")
        assertThat(p).matches("create table t of xmltype element \"E\" store all varrays as lobs allow nonschema disallow anyschema")
        assertThat(p).matches("create table t of xmltype xmltype store as binary xml xmlschema \"u.xsd\" element \"E\"")
        assertThat(p).matches("create table t of xmltype xmltype store as clob on commit preserve rows")
        assertThat(p).matches("create table t of sys.xmltype")
    }

    @Test
    fun matchesXmlTypeColumnStorage() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).matches("create table t (id number, d xmltype) xmltype d store as clob " +
            "(tablespace example storage (initial 6144) chunk 4000 nocache logging);")
        assertThat(p).matches("create table t (d xmltype) xmltype column d store as securefile clob (tablespace ts cache)")
        assertThat(p).matches("create table t (d xmltype) xmltype d store as basicfile clob seg (chunk 8192)")
        assertThat(p).matches("create table t (d xmltype) xmltype d store as clob (enable storage in row storage (initial 1m next 1m))")
        assertThat(p).matches("create table t (d xmltype) xmltype d store as securefile clob seg")
        assertThat(p).matches("create table t (d xmltype) xmltype d store as object relational " +
            "xmlschema \"http://x/w.xsd\" element \"Warehouse\"")
        assertThat(p).matches("create table t (a xmltype, b xmltype) xmltype a store as clob xmltype b store as binary xml")
        assertThat(p).matches("create table t (d xmltype) xmltype d element \"E\"")
    }

    @Test
    fun matchesAlterTableAddWithXmlTypeStorage() {
        setRootRule(DdlGrammar.ALTER_TABLE)
        assertThat(p).matches("alter table t add (d xmltype) xmltype d store as transportable binary xml;")
    }

    @Test
    fun rejectsMalformedXmlTypeStorage() {
        setRootRule(DdlGrammar.CREATE_TABLE)
        assertThat(p).notMatches("create table t of xmltype xmltype store as")
        assertThat(p).notMatches("create table t of xmltype xmltype store as transportable clob")
        assertThat(p).notMatches("create table t of xmltype xmltype store as binary")
        assertThat(p).notMatches("create table t (d xmltype) xmltype d store as object")
        assertThat(p).notMatches("create table t (d xmltype) xmltype store as clob")
        assertThat(p).notMatches("create table t of xmltype xmlschema \"u.xsd\"")
        // Oracle 26 reads any word after the storage type as the segment name (ORA-00922 at the next token).
        assertThat(p).notMatches("create table t (d xmltype) xmltype d store as clob tablespace users")
    }
}
