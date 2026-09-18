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
import org.assertj.core.api.Assertions.assertThat as assertThatAst
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateIndexTest : RuleTest() {

    @BeforeEach
    fun init() {
        setRootRule(DdlGrammar.CREATE_INDEX)
    }

    @Test
    fun matchesIndexTypesAndNames() {
        assertThat(p).matches("create index employee_ix on employees(employee_id);")
        assertThat(p).matches("create unique index employee_uk on employees(email);")
        assertThat(p).matches("create bitmap index employee_status_ix on employees(status);")
        assertThat(p).matches("create index if not exists hr.employee_ix on hr.employees(employee_id);")
        assertThat(p).matches("create json index employee_json_ix on employees(profile);")
        assertThat(p).matches("create json singlevalue index employee_profile_ix on employees e (e.profile.name.string());")
        assertThat(p).matches("create json unique dense singlevalue index employee_unique_profile_ix on employees e (e.profile.name.string());")
        assertThat(p).matches("create multivalue index employee_tags_ix on employees e (e.profile.tags.stringOnly());")
        assertThat(p).matches("create sparse index sparse_ix on some_table(some_column);")
        assertThat(p).matches("create dense index dense_ix on some_table(some_column);")
    }

    @Test
    fun matchesClusterAndTableExpressions() {
        assertThat(p).matches("create index cluster_ix on cluster hr.personnel nologging parallel 4;")
        assertThat(p).matches("create index upper_ix on hr.employees (upper(last_name));")
        assertThat(p).matches("create index function_ix on employees e (extractvalue(value(e), '/Warehouse/Area'));")
        assertThat(p).matches("create index descending_ix on employees (last_name desc, employee_id asc);")
        assertThat(p).matches("create unique index conditional_ix on orders (case when promotion_id = 2 then customer_id else null end);")
    }

    @Test
    fun matchesIndexAttributesAndIlm() {
        assertThat(p).matches("create index attrs_ix ilm add policy optimize (on should_optimize) on employees(id) pctfree 5 initrans 3 storage (initial 1m maxsize unlimited) logging online tablespace default compress advanced low nosort reverse invisible indexing partial parallel 2 annotations (add display_label 'Lookup');")
        assertThat(p).matches("create index pre_ilm_ix ilm add policy optimize after 7 days of no modification on employees(employee_id);")
        assertThat(p).matches("create index post_ilm_ix on employees(employee_id) ilm add policy optimize after 7 days of no modification;")
        assertThat(p).notMatches("create index duplicate_ilm_ix ilm add policy optimize after 7 days of no modification on employees(employee_id) ilm add policy optimize after 7 days of no modification;")
        assertThat(p).matches("create index usable_ix on employees(id) usable deferred invalidation;")
        assertThat(p).matches("create index unusable_ix on employees(id) unusable immediate invalidation;")
    }

    @Test
    fun matchesGlobalPartitioning() {
        assertThat(p).matches("create index range_ix on sales(amount_sold) global partition by range(amount_sold) (partition p1 values less than (1000), partition p2 values less than (maxvalue));")
        assertThat(p).matches("create index hash_ix on customers(last_name) global partition by hash(last_name) partitions 4;")
        assertThat(p).matches("create index hash_named_ix on customers(last_name) global partition by hash(last_name) (partition p1 tablespace tbs_1, partition p2 compress 1);")
        assertThat(p).matches("create index hash_store_ix on customers(last_name) global partition by hash(last_name) partitions 8 store in (tbs_1, tbs_2) compress 1 overflow store in (tbs_overflow);")
    }

    @Test
    fun matchesLocalPartitioning() {
        assertThat(p).matches("create index local_store_ix on products(category_id) local store in (tbs_1, tbs_2);")
        assertThat(p).matches("create bitmap index local_range_ix on products(status) local (partition p1 tablespace tbs_1 compress 1 usable, partition p2 unusable) tablespace tbs_2;")
        assertThat(p).matches("create index local_hash_ix on products(category_id) local (partition p1 tablespace tbs_1, partition p2);")
        assertThat(p).matches("create index local_composite_ix on sales(time_id, product_id) local store in (tbs_1, tbs_2) (partition p1 (subpartition sp1 tablespace tbs_1, subpartition sp2 compress 1 usable), partition p2 store in (tbs_2));")
        assertThat(p).matches("create index local_ix on products(category_id) local;")
    }

    @Test
    fun matchesBitmapJoinIndexes() {
        assertThat(p).matches("create bitmap index product_bm_ix on products p (p.category_id, s.customer_id desc) from products p, sales s where p.product_id = s.product_id;")
        assertThat(p).matches("create bitmap index product_bm_local_ix on products (category_id) from products p, sales s where p.product_id = s.product_id local (partition p1 tablespace tbs_1) tablespace tbs_2;")
    }

    @Test
    fun matchesDomainAndXmlIndexes() {
        assertThat(p).matches("create index spatial_ix on locations(geometry) indextype is spatial_indextype parallel 4 parameters ('layer_gtype=POINT');")
        assertThat(p).matches("create index local_spatial_ix on locations(geometry) indextype is hr.spatial_indextype local (partition p1 parameters ('layer_gtype=POINT'), partition p2) parameters ('layer_gtype=ANY');")
        assertThat(p).matches("create index spatial_v2_ix on locations(geometry) indextype is mdsys.spatial_index_v2;")
        val xmlIndexForms = listOf(
            "create index xml_ix on xml_docs(xml_data) indextype is xmlindex parameters ('PATH TABLE xml_path_table');",
            "create index xml_ix on xml_docs(xml_data) indextype is xdb.xmlindex parameters ('PATH TABLE xml_path_table');"
        )
        xmlIndexForms.forEach { source ->
            assertThat(p).describedAs(source).matches(source)
            val node = p.parse(source)
            assertThatAst(node.getDescendants(DdlGrammar.CREATE_INDEX_XMLINDEX_CLAUSE))
                .describedAs(source)
                .hasSize(1)
            assertThatAst(node.getDescendants(DdlGrammar.CREATE_INDEX_DOMAIN_CLAUSE))
                .describedAs(source)
                .isEmpty()
        }
        val domainIndex = p.parse("create index spatial_v2_ix on locations(geometry) indextype is mdsys.spatial_index_v2;")
        assertThatAst(domainIndex.getDescendants(DdlGrammar.CREATE_INDEX_XMLINDEX_CLAUSE)).isEmpty()
        assertThatAst(domainIndex.getDescendants(DdlGrammar.CREATE_INDEX_DOMAIN_CLAUSE)).hasSize(1)
        assertThat(p).matches("create index local_xml_ix on xml_docs(xml_data) indextype is xmlindex local (partition p1 parameters ('PARAM my_registered_parameter')) parallel;")
    }

    @Test
    fun arbitrarySchemaQualifiedXmlIndexNameUsesDomainIndexBranch() {
        val source = "create index xml_ix on xml_docs(xml_data) indextype is hr.xmlindex parameters ('PATH TABLE xml_path_table');"
        assertThat(p).matches(source)
        val node = p.parse(source)
        assertThatAst(node.getDescendants(DdlGrammar.CREATE_INDEX_XMLINDEX_CLAUSE)).isEmpty()
        assertThatAst(node.getDescendants(DdlGrammar.CREATE_INDEX_DOMAIN_CLAUSE)).hasSize(1)
    }

    @Test
    fun matchesJsonTableMultivalueIndex() {
        assertThat(p).matches("""
            create multivalue index cmvi_1 on parts_tab
              (json_table(jparts, '$.parts[*]'
                 error on error null on empty null on mismatch
                 columns (partNum number(10) path '$.partno',
                   nested path '$.subparts[*]'
                     columns (subpartNum number(20) path '$'))));
            """.trimIndent())
    }

    @Test
    fun rejectsOutOfScopeAndInvalidForms() {
        assertThat(p).notMatches("create index if exists employee_ix on employees(employee_id);")
        assertThat(p).notMatches("create search index search_ix on documents(content);")
        assertThat(p).notMatches("create indextype custom_index_type using implementation_type;")
    }
}
