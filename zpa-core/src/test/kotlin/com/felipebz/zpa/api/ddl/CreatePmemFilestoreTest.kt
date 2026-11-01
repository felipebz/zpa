//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.ddl

import com.felipebz.flr.tests.Assertions.assertThat
import com.felipebz.zpa.api.DdlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CreatePmemFilestoreTest : RuleTest() {

    @Test
    fun matchesCreateOptionsInAnyOrder() {
        setRootRule(DdlGrammar.CREATE_PMEM_FILESTORE)
        listOf(
            "create pmem filestore cloud_db_1 mountpoint '/corp/db/cloud_db_1' backingfile '/var/pmem/foo_1.' size 2t blocksize 8k autoextend on next 10g maxsize 3t",
            "create pmem filestore f1", "create pmem filestore f1 mountpoint '/a'", "create pmem filestore f1 size 2t mountpoint '/a'",
            "create pmem filestore f1 backingfile '/b' reuse", "create pmem filestore f1 blocksize 8k;", "create pmem filestore f1 autoextend off",
            "create pmem filestore f1 autoextend on", "create pmem filestore f1 autoextend on next 10g",
            "create pmem filestore f1 autoextend on maxsize unlimited", "create pmem filestore \"f1\" size 1t",
            "create pmem filestore f1 size 2t size 3t",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "create pmem filestore", "create pmem filestore f1 mountpoint", "create pmem filestore f1 mountpoint a",
            "create pmem filestore f1 backingfile", "create pmem filestore f1, f2 size 1t", "create pmem filestore s.f1 size 1t",
            "create pmem filestore f1 size 2t, blocksize 8k", "create pmem filestore f1 bogus", "create pmem filestore f1 size",
            "create pmem filestore f1 autoextend", "create pmem filestore f1 autoextend on next",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun matchesAlterOperations() {
        setRootRule(DdlGrammar.ALTER_PMEM_FILESTORE)
        listOf(
            "alter pmem filestore cloud_db_1 resize 5t;", "alter  pmem filestore cloud_db_1 mount  mountpoint '/corp/db/cloud_db_1' backingfile '/var/pmem/foo_1';",
            "alter pmem filestore cloud_db_1 dismount;", "alter pmem filestore f1 mount", "alter pmem filestore f1 mount force",
            "alter pmem filestore f1 mount mountpoint '/a' force", "alter pmem filestore f1 mount backingfile '/b' mountpoint '/a'",
            "alter pmem filestore f1 mount force mountpoint '/a'", "alter pmem filestore f1 mount force backingfile '/b'",
            "alter pmem filestore f1 mount mountpoint '/a' force backingfile '/b'",
            "alter pmem filestore f1 mount backingfile '/b' force mountpoint '/a'", "alter pmem filestore f1 mount force force",
            "alter pmem filestore f1 mount force mountpoint '/a' force", "alter pmem filestore f1 mount mountpoint '/a' force force",
            "alter pmem filestore f1 dismount force", "alter pmem filestore f1 mount mountpoint '/a' mountpoint '/b'",
            "alter pmem filestore f1 mount backingfile '/a' backingfile '/b'",
            "alter pmem filestore f1 mount mountpoint '/a' backingfile '/b' mountpoint '/c'", "alter pmem filestore f1 autoextend on next 1g maxsize 3t",
            "alter pmem filestore f1 autoextend off",
        ).forEach { assertThat(p).describedAs(it).matches(it) }
        listOf(
            "alter pmem filestore f1", "alter pmem filestore", "alter pmem filestore f1 resize", "alter pmem filestore f1 mount mountpoint",
            "alter pmem filestore f1 mount (mountpoint '/a')", "alter pmem filestore f1 bogus", "alter pmem filestore f1 resize 5t autoextend on",
            "alter pmem filestore f1 resize 5t resize 6t", "alter pmem filestore f1 dismount force force",
            "alter pmem filestore f1 dismount force mountpoint '/a'", "alter pmem filestore f1 dismount mountpoint '/a'", "alter pmem filestore f1 resize 5t mount", "alter pmem filestore s.f1 resize 5t",
        ).forEach { assertThat(p).describedAs(it).notMatches(it) }
    }

    @Test
    fun buildsPmemFilestoreNodes() {
        setRootRule(DdlGrammar.CREATE_PMEM_FILESTORE)
        assertThatAst(p.parse("create pmem filestore f1 backingfile '/b' reuse size 2t").tokens.map { it.originalValue })
            .containsExactly("create", "pmem", "filestore", "f1", "backingfile", "'/b'", "reuse", "size", "2", "t")
        setRootRule(DdlGrammar.ALTER_PMEM_FILESTORE)
        assertThatAst(p.parse("alter pmem filestore f1 mount mountpoint '/a' force").tokens.map { it.originalValue })
            .containsExactly("alter", "pmem", "filestore", "f1", "mount", "mountpoint", "'/a'", "force")
    }
}
