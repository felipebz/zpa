-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INMEMORY-JOIN-GROUP.html
CREATE INMEMORY JOIN GROUP prod_id1
  (inventories(product_id), order_items(product_id));