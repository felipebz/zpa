-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-MATERIALIZED-VIEW.html
CREATE MATERIALIZED VIEW catalog   
   REFRESH FAST START WITH SYSDATE NEXT SYSDATE + 1/4096 
   WITH PRIMARY KEY 
   AS SELECT * FROM product_information;