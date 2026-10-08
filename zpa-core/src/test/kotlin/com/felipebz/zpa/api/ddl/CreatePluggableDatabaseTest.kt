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
import com.felipebz.zpa.api.PlSqlGrammar
import com.felipebz.zpa.api.RuleTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat as assertThatAst

class CreatePluggableDatabaseTest : RuleTest() {

    private val admin = "admin user adm identified by pw"
    private val xml = "using 'x.xml'"

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_PLUGGABLE_DATABASE)
    }

    private fun matches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).matches("create pluggable database $tail")
    }

    private fun notMatches(vararg tails: String) {
        for (tail in tails) assertThat(p).describedAs(tail).notMatches("create pluggable database $tail")
    }

    @Test
    fun matchesDocumentedExamples() {
        matches(
            "CDB1_PDB2_C AS CLONE USING '/tmp/cdb1_pdb2.pdb'",
            "salespdb ADMIN USER salesadm IDENTIFIED BY password ROLES = (dba) DEFAULT TABLESPACE sales " +
                "DATAFILE '/disk1/oracle/dbs/salespdb/sales01.dbf' SIZE 250M AUTOEXTEND ON " +
                "FILE_NAME_CONVERT = ('/disk1/oracle/dbs/pdbseed/', '/disk1/oracle/dbs/salespdb/') " +
                "STORAGE (MAXSIZE 2G) PATH_PREFIX = '/disk1/oracle/dbs/salespdb/';",
            "newpdb FROM salespdb FILE_NAME_CONVERT = ('/disk1/oracle/dbs/salespdb/', '/disk1/oracle/dbs/newpdb/') " +
                "PATH_PREFIX = '/disk1/oracle/dbs/newpdb';",
            "salespdb USING '/disk1/usr/salespdb.xml' SOURCE_FILE_NAME_CONVERT = " +
                "('/disk1/oracle/dbs/salespdb/', '/disk2/oracle/dbs/salespdb/') NOCOPY STORAGE (MAXSIZE 2G) TEMPFILE REUSE;"
        )
    }

    @Test
    fun matchesNewPdbFromSeed() {
        matches(
            "p1 $admin", "p1 $admin;", "\"P1\" admin user \"Adm\" identified by \"pw\"", "p1 admin user adm identified by pw1_#$",
            "p1 $admin roles = (dba)", "p1 $admin roles = (dba, \"R\", r2)", "admin $admin", "p1 $admin parallel",
            "p1 $admin parallel 4", "p1 $admin default tablespace ts",
            "p1 $admin default tablespace ts datafile 'f' size 10m", "p1 $admin default tablespace ts datafile 'f' size 10m autoextend on",
            "p1 $admin default tablespace ts datafile 'f' size 10m reuse", "p1 $admin default tablespace ts datafile 'f', 'g'",
            "p1 $admin default tablespace ts extent management local autoallocate datafile 'f'",
            "p1 $admin default tablespace ts datafile 'f' extent management local uniform size 1m",
            "p1 $admin path_prefix = 'p'", "p1 $admin path_prefix = none", "p1 $admin path_prefix = dirobj",
            "p1 $admin tempfile reuse", "p1 $admin logging", "p1 $admin nologging",
            "p1 $admin host = 'h' port = 1", "p1 $admin port = 1 host = 'h'", "p1 $admin create_file_dest = none",
            "p1 $admin create_file_dest = '/d'", "p1 $admin create_file_dest = dg"
        )
    }

    @Test
    fun matchesStorageLimits() {
        matches(
            "p1 $admin storage unlimited", "p1 $admin storage (maxsize unlimited)", "p1 $admin storage (maxsize 2g)",
            "p1 $admin storage (max_audit_size 1g)", "p1 $admin storage (max_diag_size 1g)",
            "p1 $admin storage (maxsize 2g max_audit_size unlimited max_diag_size 1g)", "p1 $admin storage (maxsize 2 g)",
            "p1 $admin storage (maxsize 2k)", "p1 $admin storage (maxsize 2t)", "p1 $admin storage (maxsize 2p)",
            "p1 $admin storage (maxsize 2)"
        )
        notMatches(
            "p1 $admin storage", "p1 $admin storage ()", "p1 $admin storage (maxsize)", "p1 $admin storage maxsize 2g",
            "p1 $admin storage (maxsize 2g, max_audit_size 1g)", "p1 $admin storage (unlimited)",
            "p1 $admin storage (initial 1m)", "p1 $admin storage (maxsize 2x)"
        )
    }

    @Test
    fun matchesNameConversionsAndLists() {
        matches(
            "p1 $admin file_name_convert = ('a', 'b')", "p1 $admin file_name_convert = ('a', 'b', 'c', 'd')",
            "p1 $admin file_name_convert = none", "p1 $admin service_name_convert = ('a', 'b')",
            "p1 $admin service_name_convert = none", "p1 $admin service_name_convert = ('a', 'b', 'c', 'd')",
            "p1 $admin user_tablespaces = ('a')", "p1 $admin user_tablespaces = ('a', 'b')", "p1 $admin user_tablespaces = all",
            "p1 $admin user_tablespaces = all except ('a', 'b')", "p1 $admin user_tablespaces = none",
            "p1 $admin standbys = ('a', 'b')", "p1 $admin standbys = all", "p1 $admin standbys = all except ('a')",
            "p1 $admin standbys = none"
        )
        notMatches(
            "p1 $admin file_name_convert = ('a')", "p1 $admin file_name_convert = ('a', 'b', 'c')",
            "p1 $admin file_name_convert = ()", "p1 $admin file_name_convert = 'a', 'b'", "p1 $admin file_name_convert ('a', 'b')",
            "p1 $admin file_name_convert = (a, b)", "p1 $admin file_name_convert = (none)", "p1 $admin file_name_convert = ('a', 'b'),",
            "p1 $admin file_name_convert = ('a', 'b'), ('c', 'd')", "p1 $admin service_name_convert = ('a')",
            "p1 $admin user_tablespaces = (a)", "p1 $admin user_tablespaces = ()", "p1 $admin user_tablespaces = all except a",
            "p1 $admin user_tablespaces", "p1 $admin standbys = (a)", "p1 $admin standbys = ()", "p1 $admin standbys = all except a"
        )
    }

    @Test
    fun matchesOptionsInAnyOrderAndRepeated() {
        matches(
            "p1 $admin storage unlimited file_name_convert = none", "p1 $admin file_name_convert = none storage unlimited",
            "p1 $admin host = 'h' file_name_convert = none", "p1 $admin parallel 2 logging tempfile reuse path_prefix = none",
            "p1 $admin file_name_convert = none file_name_convert = ('a', 'b')", "p1 $admin logging nologging",
            "p1 $admin path_prefix = 'a' path_prefix = 'b'", "p1 $admin tempfile reuse tempfile reuse",
            "p1 $admin standbys = none file_name_convert = none user_tablespaces = all logging create_file_dest = none " +
                "service_name_convert = none path_prefix = none tempfile reuse storage unlimited " +
                "default tablespace t datafile 'f' parallel 2"
        )
    }

    @Test
    fun rejectsMalformedSeedCreation() {
        notMatches(
            "p1", "p1;", "", "p1 admin user adm", "p1 admin user adm identified pw", "p1 admin user identified by pw",
            "p1 admin user adm identified by", "p1 admin user adm identified by 'pw'", "p1 roles = (dba) $admin",
            "p1 $admin roles = ()", "p1 $admin roles = dba", "p1 $admin roles (dba)", "p1 $admin roles = ('dba')",
            "p1 $admin roles = (a.b)", "p1 $admin file_name_convert = none roles = (dba)", "p1 $admin parallel x",
            "p1 $admin default tablespace", "p1 $admin default tablespace ts datafile 'f' bigfile",
            "p1 $admin tempfile", "p1 $admin reuse", "p1 $admin path_prefix 'p'", "p1 $admin path_prefix =",
            "p1 $admin path_prefix = a.b", "p1 $admin create_file_dest 'd'", "p1 $admin create_file_dest = a.b",
            "p1 $admin host = h", "p1 $admin port = '1'", "p1 $admin file_name_convert = none x",
            "p1 $admin file_name_convert = none,", "p1 $admin filesystem_like_logging", "p1 $admin enable snapshot manual",
            "p1 $admin nocopy", "p1 $admin copy", "p1 $admin as clone", "p1 $admin source_file_name_convert = none",
            "p1 $admin decrypt using s", "p1 $admin snapshot copy", "p1 $admin no data",
            "p1 $admin refresh mode manual", "p1 $admin relocate", "p.q $admin", "p1 $admin pdb2"
        )
    }

    @Test
    fun matchesClones() {
        matches(
            "p1 from src", "p1 from src;", "p1 from src@lnk", "p1 from src @ lnk", "p1 from src@lnk.world", "p1 from src@lnk.a.b",
            "p1 from src@\"L.n\"", "p1 from \"S\"@\"L\"", "p1 from src@lnk1_#$", "p1 from non\$cdb@lnk", "p1 from non\$cdb",
            "p1 as proxy from src@lnk", "p1 as proxy from src", "p1 as proxy from src@lnk file_name_convert = none",
            "p1 from src parallel 4", "p1 from src default tablespace t", "p1 from src storage unlimited",
            "p1 from src file_name_convert = none", "p1 from src service_name_convert = none", "p1 from src path_prefix = none",
            "p1 from src tempfile reuse", "p1 from src snapshot copy", "p1 from src no data", "p1 from src snapshot copy no data",
            "p1 from src user_tablespaces = all snapshot copy", "p1 from src user_tablespaces = all no data",
            "p1 from src standbys = none logging create_file_dest = none host = 'h' port = 1",
            "p1 from src keystore identified by kp", "p1 from src keystore identified by external store",
            "p1 from src keystore identified by kp no rekey", "p1 from src keystore identified by kp rekey using 'AES256'",
            "p1 from src keystore identified by external store no rekey",
            "p1 from src storage unlimited file_name_convert = none path_prefix = none snapshot copy no data"
        )
    }

    @Test
    fun matchesCloneFromSnapshot() {
        matches(
            "p1 from src using snapshot s1", "p1 from src using snapshot \"S\"", "p1 from src using snapshot at scn 5",
            "p1 from src using snapshot at scn 5 parallel 2", "p1 from src@lnk using snapshot s1 file_name_convert = none"
        )
        notMatches(
            "p1 from src using snapshot", "p1 from src using snapshot 's'", "p1 from src using snapshot at scn",
            "p1 from src using snapshot at timestamp '2020-01-01 00:00:00'", "p1 from src using snapshot at '2020-01-01'",
            "p1 from src using snapshot at systimestamp", "p1 from src using snapshot at 5",
            "p1 from src using snapshot s1 using snapshot s2", "p1 from src parallel 2 using snapshot s1"
        )
    }

    @Test
    fun matchesRemoteOnlyCloneClauses() {
        matches(
            "p1 from src@lnk refresh mode manual", "p1 from src@lnk refresh mode every 5 minutes",
            "p1 from src@lnk refresh mode every 5 hours", "p1 from src@lnk refresh mode none",
            "p1 from src@lnk refresh mode manual file_name_convert = none", "p1 from src@lnk file_name_convert = none refresh mode manual",
            "p1 as proxy from src@lnk refresh mode manual", "p1 from src@lnk relocate", "p1 from src@lnk relocate keep source",
            "p1 from src@lnk relocate availability max", "p1 from src@lnk relocate availability normal",
            "p1 from src@lnk relocate keep source availability max", "p1 from src@lnk relocate refresh mode manual",
            "p1 from src@lnk relocate keep source availability normal refresh mode every 5 minutes",
            "p1 from src@lnk snapshot copy relocate"
        )
        notMatches(
            "p1 from src refresh mode manual", "p1 from src relocate", "p1 from src relocate keep source",
            "p1 from src@lnk refresh mode every 5", "p1 from src@lnk refresh mode", "p1 from src@lnk refresh mode every 5 days",
            "p1 from src@lnk relocate availability max keep source", "p1 from src@lnk relocate availability high",
            "p1 from src@lnk relocate availability", "p1 from src@lnk relocate keep", "p1 from src@lnk enable snapshot manual"
        )
    }

    @Test
    fun rejectsMalformedClones() {
        notMatches(
            "p1 from", "p1 from src@", "p1 from @lnk", "p1 from a.b", "p1 from 'src'", "p1 from src, other", "p1 from src@1lnk",
            "p1 from src@'lnk'", "p1 from select", "p1 from from", "p1 from src@lnk@x", "p1 from src nocopy", "p1 from src copy",
            "p1 from src as clone", "p1 from src admin user a identified by p", "p1 from src roles = (dba)",
            "p1 from src source_file_name_convert = none", "p1 from src decrypt using s", "p1 from src filesystem_like_logging",
            "p1 from src keystore", "p1 from src keystore identified by", "p1 from src keystore identified by 'kp'",
            "p1 from src keystore identified by kp no rekey rekey using 'x'",
            "p1 as proxy", "p1 as proxy src@lnk", "p1 proxy from src@lnk"
        )
    }

    @Test
    fun matchesPlugInFromXml() {
        matches(
            "p1 $xml", "p1 $xml;", "p1 as clone $xml", "p1 using q'[x.xml]'", "p1 using N'x'",
            "p1 $xml source_file_name_convert = ('a', 'b')", "p1 $xml source_file_name_convert = none",
            "p1 $xml source_file_directory = 'd'", "p1 $xml source_file_directory = none",
            "p1 $xml source_file_name_convert = none source_file_directory = 'd'",
            "p1 $xml copy", "p1 $xml move", "p1 $xml nocopy", "p1 $xml copy file_name_convert = ('a', 'b')",
            "p1 $xml move file_name_convert = ('a', 'b')", "p1 $xml file_name_convert = ('a', 'b') copy",
            "p1 $xml file_name_convert = none", "p1 $xml service_name_convert = none", "p1 $xml default tablespace t",
            "p1 $xml storage unlimited", "p1 $xml path_prefix = none", "p1 $xml tempfile reuse",
            "p1 $xml user_tablespaces = all", "p1 $xml user_tablespaces = all copy", "p1 $xml user_tablespaces = all move",
            "p1 $xml user_tablespaces = ('a') nocopy", "p1 $xml standbys = none", "p1 $xml logging", "p1 $xml create_file_dest = none",
            "p1 $xml host = 'h'", "p1 $xml port = 1", "p1 $xml decrypt using s", "p1 $xml parallel",
            "p1 $xml source_file_name_convert = ('a', 'b') nocopy storage (maxsize 2g) tempfile reuse",
            "p1 $xml tempfile reuse decrypt using s host = 'h'", "p1 as clone $xml nocopy"
        )
    }

    @Test
    fun rejectsMalformedXmlPlugIn() {
        notMatches(
            "p1 using", "p1 using x", "p1 using \"x\"", "p1 using 'a' 'b'", "p1 as clone", "p1 as clone using", "p1 using 'x.xml' as clone",
            "p1 $xml source_file_name_convert = ('a')", "p1 $xml source_file_directory = d", "p1 $xml decrypt using 's'",
            "p1 $xml decrypt using", "p1 $xml decrypt", "p1 $xml admin user a identified by p", "p1 $xml roles = (dba)",
            "p1 $xml filesystem_like_logging", "p1 $xml enable snapshot manual", "p1 $xml snapshot copy", "p1 $xml no data",
            "p1 $xml refresh mode manual", "p1 $xml relocate"
        )
    }

    @Test
    fun restrictsDefaultTablespaceFilesToSeedCreation() {
        matches(
            "p1 $admin default tablespace t", "p1 $admin default tablespace t datafile 'f'",
            "p1 $admin default tablespace t extent management local", "p1 $admin default tablespace t extent management local datafile 'f'",
            "p1 $admin default tablespace t datafile 'f' extent management local autoallocate", "p1 from src default tablespace t",
            "p1 $xml default tablespace t nocopy"
        )
        for (mode in listOf("from src", xml)) {
            notMatches(
                "p1 $mode default tablespace t datafile 'f'", "p1 $mode default tablespace t datafile 'f' size 1m",
                "p1 $mode default tablespace t extent management local",
                "p1 $mode default tablespace t extent management local datafile 'f'", "p1 $mode default tablespace t datafile",
                "p1 $mode default tablespace t datafile 'f', 'g'"
            )
        }
    }

    @Test
    fun keepsBothSourceFileClausesTogether() {
        matches(
            "p1 $xml source_file_name_convert = none source_file_directory = none",
            "p1 $xml source_file_directory = none source_file_name_convert = none",
            "p1 $xml source_file_name_convert = ('a', 'b') source_file_name_convert = ('c', 'd')",
            "p1 $xml source_file_directory = 'd' source_file_directory = 'e'", "p1 $xml nocopy source_file_name_convert = none",
            "p1 $xml source_file_name_convert = none nocopy", "p1 $xml source_file_directory = 'd' nocopy"
        )
        notMatches("p1 $admin source_file_name_convert = none", "p1 from src source_file_directory = none")
    }

    @Test
    fun restrictsCopyModesInXmlPlugIn() {
        matches(
            "p1 $xml copy", "p1 $xml move", "p1 $xml nocopy", "p1 $xml copy file_name_convert = ('a', 'b')",
            "p1 $xml move file_name_convert = ('a', 'b')", "p1 $xml file_name_convert = ('a', 'b') copy",
            "p1 $xml file_name_convert = ('a', 'b') move", "p1 $xml file_name_convert = none nocopy",
            "p1 $xml file_name_convert = none service_name_convert = none nocopy", "p1 $xml copy storage unlimited file_name_convert = ('a', 'b')",
            "p1 $xml copy logging file_name_convert = ('a', 'b')", "p1 $xml file_name_convert = ('a', 'b') storage unlimited copy",
            "p1 $xml nocopy storage unlimited", "p1 $xml storage unlimited nocopy", "p1 $xml copy copy", "p1 $xml nocopy nocopy",
            "p1 $xml move move", "p1 $xml copy logging copy"
        )
        notMatches(
            "p1 $xml nocopy file_name_convert = ('a', 'b')", "p1 $xml file_name_convert = ('a', 'b') nocopy",
            "p1 $xml nocopy file_name_convert = none", "p1 $xml copy nocopy", "p1 $xml nocopy copy", "p1 $xml move nocopy",
            "p1 $xml nocopy move", "p1 $xml copy move", "p1 $xml move copy",
            "p1 $xml nocopy storage unlimited file_name_convert = ('a', 'b')",
            "p1 $xml file_name_convert = ('a', 'b') storage unlimited nocopy",
            "p1 $xml service_name_convert = none nocopy file_name_convert = none", "p1 $xml copy storage unlimited nocopy",
            "p1 $admin copy", "p1 from src move", "p1 from src nocopy file_name_convert = none"
        )
    }

    @Test
    fun restrictsUserTablespaceSuffixesByMode() {
        for (mode in listOf(admin, "from src", xml)) {
            matches(
                "p1 $mode user_tablespaces = all", "p1 $mode user_tablespaces = all snapshot copy",
                "p1 $mode user_tablespaces = all no data", "p1 $mode user_tablespaces = ('a') snapshot copy",
                "p1 $mode user_tablespaces = all except ('a') no data"
            )
        }
        matches(
            "p1 $xml user_tablespaces = all copy", "p1 $xml user_tablespaces = all move", "p1 $xml user_tablespaces = all nocopy",
            "p1 $xml user_tablespaces = ('a') nocopy", "p1 $xml user_tablespaces = none copy",
            "p1 $xml user_tablespaces = all except ('a') move", "p1 $xml user_tablespaces = all snapshot copy copy"
        )
        for (mode in listOf(admin, "from src")) {
            notMatches(
                "p1 $mode user_tablespaces = all copy", "p1 $mode user_tablespaces = all move",
                "p1 $mode user_tablespaces = all nocopy", "p1 $mode user_tablespaces = ('a') nocopy",
                "p1 $mode user_tablespaces = none copy", "p1 $mode user_tablespaces = all except ('a') move",
                "p1 $mode user_tablespaces = all copy move", "p1 $mode user_tablespaces = all snapshot copy copy"
            )
        }
        notMatches("p1 $xml user_tablespaces = all copy move", "p1 $xml user_tablespaces = all copy nocopy",
            "p1 $xml user_tablespaces = all nocopy file_name_convert = ('a', 'b')")
    }

    @Test
    fun keepsOutOfScopeCreationModesUnsupported() {
        notMatches(
            "p1 as application container $admin", "as seed $admin", "p1 from b@l using mirror copy m",
            "p1 container_map update add partition p", "using snapshot s1"
        )
    }

    @Test
    fun buildsDedicatedNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "create pluggable database a admin user u identified by p roles = (dba) storage (maxsize 2g) path_prefix = 'x';\n" +
                "create pluggable database b from a@lnk using snapshot s1 file_name_convert = none refresh mode manual;\n" +
                "create pluggable database c as clone using 'c.xml' nocopy tempfile reuse;\n")
        assertThatAst(tree.getDescendants(DdlGrammar.CREATE_PLUGGABLE_DATABASE)).hasSize(3)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_FROM_SEED)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_CLONE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_FROM_XML)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_ROLES_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_STORAGE_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_PATH_PREFIX)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_USING_SNAPSHOT)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_FILE_NAME_CONVERT)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_REFRESH_MODE_CLAUSE)).hasSize(1)
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_TEMPFILE_REUSE)).hasSize(1)
        val xml = tree.getFirstDescendant(DdlGrammar.PDB_FROM_XML)!!
        assertThatAst(xml.children.map { it.tokenOriginalValue.lowercase() }).containsExactly(
            "as", "clone", "using", "'c.xml'", "nocopy", "tempfile")
    }

    @Test
    fun matchesKeystoreInEveryCreationMode() {
        matches(
            "p1 $admin keystore identified by kp", "p1 $admin keystore identified by external store",
            "p1 $admin tempfile reuse keystore identified by kp default tablespace t",
            "p1 $xml nocopy keystore identified by kp decrypt using s", "p1 $xml decrypt using s keystore identified by kp nocopy",
            "p1 as clone $xml keystore identified by external store", "p1 $xml keystore identified by kp copy file_name_convert = none",
            "p1 $admin keystore identified by a keystore identified by b",
            "CDB1_PDB2 USING '/tmp/cdb1_pdb2.xml' NOCOPY KEYSTORE IDENTIFIED BY keystore_password DECRYPT USING transport_secret"
        )
    }

    @Test
    fun rejectsMalformedOrRekeyKeystoreOutsideClones() {
        notMatches(
            "p1 $admin keystore", "p1 $admin keystore identified by", "p1 $admin keystore identified by 'kp'",
            "p1 $admin keystore identifed by kp", "p1 $admin keystore identified by kp no rekey",
            "p1 $admin keystore identified by kp rekey using 'a'", "p1 $xml keystore identified by kp no rekey",
            "p1 $xml keystore identified by kp rekey using 'a'", "p1 $xml keystore", "p1 $xml keystore identified by"
        )
    }

    @Test
    fun matchesContainerMapUpdates() {
        val add = "container_map update (add partition q values less than (100))"
        matches(
            "p1 $admin $add", "p1 $admin file_name_convert = ('a', 'b') $add", "p1 $admin $add file_name_convert = ('a', 'b')",
            "p1 $admin $add tempfile reuse storage unlimited", "p1 from src $add", "p1 as proxy from src@l $add", "p1 $xml nocopy $add",
            "p1 $admin container_map update (add partition values less than (maxvalue))",
            "p1 $admin container_map update (add partition q values less than (100), partition r values less than (200))",
            "p1 $admin container_map update (add partition q values less than (100) tablespace t)",
            "p1 $admin container_map update (split partition q at (50) into (partition q, partition r))",
            "p1 $admin container_map update (split partition q at (50))",
            "p1 $admin container_map update (split partition q values (1, 2) into (partition q, partition r))",
            "p1 $admin container_map update (split partition q into (partition q values less than (50), partition r))",
            "p1 $admin $add keystore identified by kp", "p1 $admin $add $add"
        )
    }

    @Test
    fun rejectsMalformedContainerMapUpdates() {
        notMatches(
            "p1 container_map update (add partition q values less than (100))", "p1 $admin container_map",
            "p1 $admin container_map update", "p1 $admin container_map update ()",
            "p1 $admin container_map (add partition q values less than (100))",
            "p1 $admin container_map update add partition q values less than (100)",
            "p1 $admin container_map update (add partition q values less than (100)",
            "p1 $admin container_map update (drop partition q)", "p1 $admin container_map update (merge partitions a, b into partition c)",
            "p1 $admin container_map update (split partition q at (50) into (partition q, partition r)"
        )
    }

    @Test
    fun buildsKeystoreAndContainerMapNodes() {
        setRootRule(PlSqlGrammar.FILE_INPUT)
        val tree = p.parse(
            "create pluggable database a using 'a.xml' nocopy keystore identified by kp decrypt using s;\n" +
                "create pluggable database b admin user u identified by p " +
                "container_map update (split partition q at (50) into (partition q, partition r));\n" +
                "create pluggable database d admin user u identified by p " +
                "container_map update (add partition q values ('a') tablespace t);\n" +
                "create pluggable database c from a keystore identified by kp no rekey;\n")
        val xml = tree.getFirstDescendant(DdlGrammar.PDB_FROM_XML)!!
        assertThatAst(xml.children.map { it.name }).containsExactly(
            "USING", "CHARACTER_LITERAL", "NOCOPY", "PDB_KEYSTORE_CLAUSE", "PDB_DECRYPT_CLAUSE")
        assertThatAst(xml.getFirstDescendant(DdlGrammar.PDB_KEYSTORE_CLAUSE)!!.children.map { it.name })
            .containsExactly("KEYSTORE", "IDENTIFIED", "BY", "IDENTIFIER_NAME")
        val map = tree.getFirstDescendant(DdlGrammar.PDB_CONTAINER_MAP_CLAUSE)!!
        assertThatAst(map.children.map { it.name }).containsExactly(
            "CONTAINER_MAP", "UPDATE", "LPARENTHESIS", "SPLIT_TABLE_PARTITION", "RPARENTHESIS")
        assertThatAst(tree.getDescendants(DdlGrammar.PDB_KEYSTORE_CLAUSE)).hasSize(2)
        val rekey = tree.getFirstDescendant(DdlGrammar.PDB_REKEY_CLAUSE)!!
        assertThatAst(rekey.children.map { it.name }).containsExactly("NO", "REKEY")
        val clone = tree.getFirstDescendant(DdlGrammar.PDB_CLONE)!!
        assertThatAst(clone.children.map { it.name }).containsExactly(
            "FROM", "IDENTIFIER_NAME", "PDB_KEYSTORE_CLAUSE", "PDB_REKEY_CLAUSE")
    }

    @Test
    fun matchesRekeyAsIndependentCloneOptions() {
        matches(
            "p1 from src no rekey", "p1 from src keystore identified by kp no rekey", "p1 from src no rekey keystore identified by kp",
            "p1 from src keystore identified by kp file_name_convert = ('a', 'b') no rekey",
            "p1 from src keystore identified by kp tempfile reuse no rekey", "p1 from src no rekey tempfile reuse no rekey",
            "p1 from src rekey", "p1 from src rekey tempfile reuse", "p1 from src rekey using 'AES256'", "p1 from src rekey using aes256",
            "p1 from src rekey using definitely_not_an_algorithm keystore identified by kp",
            "p1 from src keystore identified by kp rekey using 'AES256' tempfile reuse",
            "p1 from src rekey rekey", "p1 from src rekey using 'a' rekey using 'b'", "p1 from src no rekey no rekey",
            "p1 from src no data no rekey", "p1 from src@l no rekey", "p1 from src@l keystore identified by kp rekey using aes256",
            "p1 as proxy from src@l no rekey", "p1 from src@l no rekey refresh mode manual", "p1 from src@l refresh mode manual rekey"
        )
    }

    @Test
    fun rejectsMixedOrMalformedRekeyAndRekeyOutsideClones() {
        notMatches(
            "p1 from src no rekey rekey", "p1 from src no rekey rekey using 'a'", "p1 from src rekey no rekey",
            "p1 from src rekey using 'a' no rekey", "p1 from src no rekey tempfile reuse rekey using 'a'", "p1 from src no",
            "p1 from src rekey using", "p1 from src rekey using 1", "p1 from src rekey using a.b", "p1 from src rekey using 'a' 'b'",
            "p1 from src rekey 'a'", "p1 from src rekey using ('a')",
            "p1 $admin no rekey", "p1 $admin rekey", "p1 $admin rekey using 'a'", "p1 $xml nocopy no rekey", "p1 $xml nocopy rekey using 'a'"
        )
    }

    @Test
    fun matchesContainerMapAddPartitionShapes() {
        listOf(
            "add partition", "add partition q", "add partition q, partition r", "add partition q values ('a')",
            "add partition q values ('a', 'b')", "add partition q values (default)", "add partition q values (null, 'a')",
            "add partition q values (('a', 1), ('b', 2))", "add partition q values ('a'), partition r values ('b')",
            "add partition q values less than (1), partition r values ('b')", "add partition q tablespace t",
            "add partition q values less than (100) tablespace t compress", "add partition q values ('a') (subpartition s)",
            "add partition q values less than (100) update indexes", "add partition q values ('a') update indexes",
            "add partition q values less than (100) parallel 2", "add partition q values less than (100) noparallel",
            "add partition q values less than (100) parallel 2 update indexes",
            "add partition q values less than (100, 'z')", "add partition values ('a')",
        ).forEach { matches("p1 $admin container_map update ($it)") }
    }

    @Test
    fun rejectsMalformedContainerMapAddPartitions() {
        listOf(
            "add partition q values", "add partition q values less", "add partition q values less than", "add partition q values less than ()",
            "add partition q values ()", "add partition q values 1", "add partition q values less than (100),",
            "add partition q before r", "add partition q values less than (100) online", "add partition q hash",
            "add q values less than (100)", "add partitions 2", "add partition q values less than (100) partition r values less than (200)",
        ).forEach { notMatches("p1 $admin container_map update ($it)") }
    }

    @Test
    fun matchesContainerMapSplitSuffixesOracleParses() {
        listOf(
            "split partition q at (50) online", "split partition q at (50) into (partition q, partition r) online",
            "split partition q values (1) into (partition q, partition r) online",
            "split partition q at (50) update indexes", "split partition q at (50) update global indexes parallel 2 online",
        ).forEach { matches("p1 $admin container_map update ($it)") }
    }

    @Test
    fun rejectsContainerMapSplitSuffixesOracleRejects() {
        listOf(
            "split partition q at (50) parallel 2", "split partition q at (50) noparallel",
            "split partition q at (50) into (partition q, partition r) parallel 2",
            "split partition q at (50) into (partition q, partition r) nested table n store as s",
            "split partition q at (50) into (partition q, partition r) online parallel 2",
            "split partition q at (50) into (partition q, partition r) foo",
        ).forEach { notMatches("p1 $admin container_map update ($it)") }
    }
}
