CREATE MATERIALIZED ZONEMAP sales_zmap
  ON sales(cust_id, prod_id);