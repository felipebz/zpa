-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-HIERARCHY.html
CREATE OR REPLACE HIERARCHY time_hier  -- Hierarchy name
USING time_attr_dim               -- Refers to TIME_ATTR_DIM attribute dimension
 (month CHILD OF                  -- Months in the attribute dimension
 quarter CHILD OF
 year);