-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Type-Constructor-Expressions.html
CREATE TABLE warehouse_tab OF warehouse_typ;
INSERT INTO warehouse_tab 
   VALUES (warehouse_typ(101, 'new_wh', 201));