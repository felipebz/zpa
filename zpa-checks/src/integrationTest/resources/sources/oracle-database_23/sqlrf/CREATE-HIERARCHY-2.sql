-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-HIERARCHY.html
CREATE OR REPLACE HIERARCHY geography_hier
USING geography_attr_dim
 (state_province
  CHILD OF country
  CHILD OF region);