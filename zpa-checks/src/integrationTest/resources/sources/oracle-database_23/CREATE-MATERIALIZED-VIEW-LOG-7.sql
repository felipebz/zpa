-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-MATERIALIZED-VIEW-LOG.html
CREATE MATERIALIZED VIEW products_mv 
   REFRESH FAST ON COMMIT
   AS SELECT SUM(list_price - min_price), category_id
         FROM product_information 
         GROUP BY category_id;