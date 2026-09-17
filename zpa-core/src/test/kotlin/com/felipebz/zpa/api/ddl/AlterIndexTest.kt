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

class AlterIndexTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.ALTER_INDEX)
    }

    @Test
    fun matchesGeneralIndexAttributes() {
        assertThat(p).matches("alter index if exists hr.employee_ix initrans 5 nologging parallel 4 indexing partial;")
        assertThat(p).matches("alter index employee_ix initrans 5 storage (initial 64k next 1m maxextents 20);")
    }

    @Test
    fun matchesRebuildOptions() {
        assertThat(p).matches("alter index employee_ix rebuild reverse parallel;")
        assertThat(p).matches("alter index employee_ix rebuild partition p1 online tablespace indexes parameters ('format=basic') pctfree 5 compress advanced low logging indexing full;")
        assertThat(p).matches("alter index employee_ix rebuild subpartition sp1 nocompress noparallel;")
        assertThat(p).matches("alter index employee_ix rebuild deferred invalidation;")
        assertThat(p).matches("alter index employee_ix rebuild immediate invalidation;")
        assertThat(p).matches("alter index employee_ix rebuild online deferred invalidation;")
        assertThat(p).matches("alter index employee_ix rebuild partition p1 immediate invalidation;")
    }

    @Test
    fun matchesIndexStateAndMaintenanceActions() {
        assertThat(p).matches("alter index employee_ix parameters ('format=basic');")
        assertThat(p).matches("alter index xml_ix parameters ('PARAM my_registered_parameter');")
        assertThat(p).matches("alter index xml_ix parameters ('PATH TABLE xml_path_table');")
        assertThat(p).matches("alter index xml_ix rebuild parameters ('ADD_GROUP GROUP po_item XMLTable po_idx_tab ''/PurchaseOrder'' COLUMNS reference VARCHAR2(30) PATH ''Reference''');")
        assertThat(p).matches("alter index employee_ix compile;")
        assertThat(p).matches("alter index employee_ix disable;")
        assertThat(p).matches("alter index employee_ix enable;")
        assertThat(p).matches("alter index employee_ix unusable online deferred invalidation;")
        assertThat(p).matches("alter index employee_ix unusable immediate invalidation;")
        assertThat(p).matches("alter index employee_ix visible;")
        assertThat(p).matches("alter index employee_ix invisible;")
        assertThat(p).matches("alter index employee_ix rename to employee_ix_new;")
        assertThat(p).matches("alter index employee_ix coalesce cleanup only parallel 2;")
        assertThat(p).matches("alter index employee_ix nomonitoring usage;")
        assertThat(p).matches("alter index employee_ix update block references;")
    }

    @Test
    fun matchesSpaceAndCompressionClauses() {
        assertThat(p).matches("alter index employee_ix deallocate unused keep 64k;")
        assertThat(p).matches("alter index employee_ix deallocate unused keep 1000;")
        assertThat(p).matches("alter index employee_ix allocate extent;")
        assertThat(p).matches("alter index employee_ix allocate extent (size 1024);")
        assertThat(p).matches("alter index employee_ix allocate extent (size 1m datafile '/u01/index01.dbf' instance 2);")
        assertThat(p).matches("alter index employee_ix modify partition p1 allocate extent (instance 2 size 1m);")
        assertThat(p).matches("alter index employee_ix shrink space compact cascade;")
        assertThat(p).notMatches("alter index employee_ix allocate extent (size 1m) datafile '/u01/index01.dbf' instance 2;")
    }

    @Test
    fun matchesStorageOptionsApplicableToIndexes() {
        assertThat(p).matches("alter index employee_ix storage (maxsize 1g);")
        assertThat(p).matches("alter index employee_ix storage (maxsize unlimited flash_cache keep (cell_flash_cache (none)));")
        assertThat(p).matches("alter index employee_ix modify partition p1 storage (maxsize 512m flash_cache none (cell_flash_cache (default)));")
        assertThat(p).notMatches("alter index employee_ix storage (encrypt);")
        assertThat(p).notMatches("alter index employee_ix storage (cell_flash_cache (none));")
    }

    @Test
    fun matchesPartitionMaintenanceClauses() {
        assertThat(p).matches("alter index cost_ix modify partition p2 unusable;")
        assertThat(p).matches("alter index cost_ix rebuild partition p2;")
        assertThat(p).matches("alter index cost_ix modify partition p3 storage (maxextents 30) logging;")
        assertThat(p).matches("alter index cost_ix modify partition p3 compress advanced low;")
        assertThat(p).matches("alter index cost_ix modify partition p3 parameters ('format=basic');")
        assertThat(p).matches("alter index cost_ix modify partition p3 coalesce cleanup parallel;")
        assertThat(p).matches("alter index cost_ix modify partition p3 update block references;")
        assertThat(p).matches("alter index cost_ix rename partition p3 to p3_q3;")
        assertThat(p).matches("alter index cost_ix rename subpartition sp3 to sp3_new;")
        assertThat(p).matches("alter index cost_ix drop partition p1;")
        assertThat(p).matches("alter index cost_ix split partition p2 at (1500) into (partition p2a tablespace tbs_01 logging, partition p2b tablespace tbs_02);")
        assertThat(p).matches("alter index cost_ix coalesce partition parallel;")
        assertThat(p).matches("alter index cost_ix add partition p4 tablespace tbs_01 compress 1 parallel 2;")
        assertThat(p).matches("alter index cost_ix modify subpartition sp1 allocate extent;")
        assertThat(p).matches("alter index cost_ix modify subpartition sp1 deallocate unused keep 1000;")
        assertThat(p).matches("alter index cost_ix modify subpartition sp1 unusable;")
        assertThat(p).matches("alter index cost_ix modify default attributes for partition p1 tablespace default initrans 5 nologging;")
    }

    @Test
    fun matchesIlmAndAnnotations() {
        assertThat(p).matches("alter index employee_ix ilm add policy segment tier to low_cost_tbs;")
        assertThat(p).matches("alter index employee_ix ilm (add policy);")
        assertThat(p).matches("alter index employee_ix ilm add policy;")
        assertThat(p).matches("alter index employee_ix ilm (add policy segment tier to low_cost_tbs my_custom_ado_rules);")
        assertThat(p).matches("alter index employee_ix ilm add policy segment tier to low_cost_tbs my_custom_ado_rules;")
        assertThat(p).matches("alter index employee_ix ilm delete policy old_policy;")
        assertThat(p).matches("alter index employee_ix ilm enable policy p1;")
        assertThat(p).matches("alter index employee_ix ilm disable policy p1;")
        assertThat(p).matches("alter index employee_ix ilm delete_all;")
        assertThat(p).matches("alter index employee_ix ilm enable_all;")
        assertThat(p).matches("alter index employee_ix ilm disable_all;")
        assertThat(p).matches("alter index employee_ix ilm add policy optimize after 90 days of no access;")
        assertThat(p).matches("alter index employee_ix ilm add policy optimize (on should_optimize);")
        assertThat(p).matches("alter index employee_ix ilm add policy optimize (on hr.should_optimize);")
        assertThat(p).matches("alter index employee_ix annotations (add if not exists display_label 'Employee index', drop if exists obsolete, replace description 'Lookup index');")
    }

    @Test
    fun rejectsIfNotExistsForAlterIndex() {
        assertThat(p).notMatches("alter index if not exists employee_ix unusable;")
    }
}
