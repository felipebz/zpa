-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-MATERIALIZED-VIEW.html
CREATE MATERIALIZED VIEW order_data REFRESH WITH ROWID 
   AS SELECT * FROM orders;