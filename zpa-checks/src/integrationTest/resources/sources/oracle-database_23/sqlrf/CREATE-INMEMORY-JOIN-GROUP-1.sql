-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INMEMORY-JOIN-GROUP.html
CREATE INMEMORY JOIN GROUP prod_id2
  (inventories(product_id), pm.online_media(product_id));