-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-MATERIALIZED-ZONEMAP.html
CREATE MATERIALIZED ZONEMAP sales_zmap
  ON sales(cust_id, prod_id);