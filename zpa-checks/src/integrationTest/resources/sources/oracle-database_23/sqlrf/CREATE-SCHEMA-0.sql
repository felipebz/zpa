-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-SCHEMA.html
CREATE SCHEMA AUTHORIZATION oe
   CREATE TABLE new_product 
      (color VARCHAR2(10)  PRIMARY KEY, quantity NUMBER) 
   CREATE VIEW new_product_view 
      AS SELECT color, quantity FROM new_product WHERE color = 'RED' 
   GRANT select ON new_product_view TO hr;