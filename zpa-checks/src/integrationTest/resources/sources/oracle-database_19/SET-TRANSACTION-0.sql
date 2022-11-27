-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SET-TRANSACTION.html
COMMIT; 

SET TRANSACTION READ ONLY NAME 'Toronto'; 

SELECT product_id, quantity_on_hand FROM inventories
   WHERE warehouse_id = 5
   ORDER BY product_id; 

COMMIT;