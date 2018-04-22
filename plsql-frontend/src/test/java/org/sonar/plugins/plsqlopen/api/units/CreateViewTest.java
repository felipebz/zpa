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
package org.sonar.plugins.plsqlopen.api.units;

import static org.sonar.sslr.tests.Assertions.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.RuleTest;

public class CreateViewTest extends RuleTest {

    @Before
    public void init() {
        setRootRule(PlSqlGrammar.CREATE_VIEW);
    }

    @Test
    public void matchesSimpleView() {
        assertThat(p).matches("create view foo as select 1 from dual;");
    }
    
    @Test
    public void matchesSimpleViewWithoutSemicolon() {
        assertThat(p).matches("create view foo as select 1 from dual");
    }
    
    @Test
    public void matchesSimpleCreateOrReplaceView() {
        assertThat(p).matches("create or replace view foo as select 1 from dual;");
    }
    
    @Test
    public void matchesViewWithSchema() {
        assertThat(p).matches("create view sch.foo as select 1 from dual;");
    }

    @Test
    public void matchesEditionableView() {
        assertThat(p).matches("create editionable view foo as select 1 from dual;");
    }

    @Test
    public void matchesNonEditionableView() {
        assertThat(p).matches("create noneditionable view foo as select 1 from dual;");
    }
    
    @Test
    public void matchesForceView() {
        assertThat(p).matches("create force view foo as select 1 from dual;");
    }
    
    @Test
    public void matchesNoForceView() {
        assertThat(p).matches("create no force view foo as select 1 from dual;");
    }
    
    @Test
    public void matchesViewWithColumnAliases() {
        assertThat(p).matches("create view foo(a, b, c) as select 1, 2, 3 from dual;");
    }
    
    @Test
    public void matchesViewWithReadOnly() {
        assertThat(p).matches("create view foo as select 1, 2, 3 from dual with read only;");
    }
    
    @Test
    public void matchesViewWithCheckOption() {
        assertThat(p).matches("create view foo as select 1, 2, 3 from dual with check option;");
    }
    
    @Test
    public void matchesViewWithCheckOptionAndName() {
        assertThat(p).matches("create view foo as select 1, 2, 3 from dual with check option constraint cons_name;");
    }
    
    @Test @Ignore
    public void matchesViewWithConstraints() {
        assertThat(p).matches(lines(
                "create view emp_sal (emp_id, last_name," ,
                                     "email unique rely disable novalidate,",
                                     "constraint id_pk primary key (emp_id) rely disable novalidate)",
                 "as select employee_id, last_name, email from employees;"));
    }
    
    @Test @Ignore
    public void matchesObjectView() {
        assertThat(p).matches(lines(
                "create or replace view oc_inventories of inventory_typ",
                "with object oid (product_id)",
                "as select product_id from inventories;"));
    }
    
    
    @Test @Ignore
    public void matchesXmlView() {
        assertThat(p).matches(lines(
                "create view warehouse_view of xmltype",
                "xmlschema \"http://www.example.com/xwarehouses.xsd\"", 
                   "element \"warehouse\"",
                   "with object id", 
                   "(extract(object_value, '/warehouse/area/text()').getnumberval())",
                "as select xmlelement(\"warehouse\",",
                          "xmlforest(warehouseid as \"building\",",
                                    "area as \"area\",",
                                    "docks as \"docks\",",
                                    "docktype as \"docktype\",",
                                    "wateraccess as \"wateraccess\",",
                                    "railaccess as \"railaccess\",",
                                    "parking as \"parking\",",
                                    "vclearance as \"vclearance\"))",
                "from warehouse_table;"));
    }

    @Test
    public void matchesDecode() {
    	assertThat(p).matches("create or replace view foo as select decode(bp.reference,null,sp.name1,bp.name1) p_name1 from bp, sp;");
    }
    
    @Test
    public void matchesDecodeWithRefColumn() {
    	assertThat(p).matches("create or replace view foo as select decode(bp.ref,null,sp.name1,bp.name1) p_name1 from bp, sp;");
    }
    
    @Test
    public void matchesViewWithOrder() {
    	assertThat(p).matches("create or replace view foo as (select abc,1 from dual) order by abc;");
    }
    
    @Test
    public void matchesMaterializedView() {
    	assertThat(p).matches("create materialized view foo as select 1 from dual;");
    }
    @Test
    public void matchesMaterializedViewComplex() {
    	assertThat(p).matches("create materialized view foo pctfree 0 tablespace dat3 refresh complete start with sysdate+2/24 next trunc(sysdate)+1 as select 1 from dual;");
    }
    @Test
    public void matchesMaterializedViewWithTablespaceAndRefresh() {
    	assertThat(p).matches("create materialized view foo tablespace dat3 refresh complete as select 1 from dual;");
    }
    
    @Test
    public void notMatchesMaterializedView() {
    	assertThat(p).notMatches("create materialized force view foo as select 1 from dual;");
    }
}
