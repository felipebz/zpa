-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-HIERARCHY.html
CREATE OR REPLACE HIERARCHY product_hier
USING product_attr_dim
 (category
  CHILD OF department);