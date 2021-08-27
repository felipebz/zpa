-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ALTER-MATERIALIZED-VIEW.html
ALTER MATERIALIZED VIEW order_data 
   REFRESH WITH PRIMARY KEY;