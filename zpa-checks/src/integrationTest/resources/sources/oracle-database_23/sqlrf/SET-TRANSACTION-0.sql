-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SET-TRANSACTION.html
COMMIT;
SET TRANSACTION READ ONLY NAME 'West Coast';
SELECT product_id, quantity_on_hand, 'San Francisco' location
  FROM inventories
    WHERE warehouse_id = 2
    ORDER BY product_id;
SELECT product_id, quantity_on_hand, 'Seattle' location
  FROM inventories
    WHERE warehouse_id = 4
    ORDER BY product_id;
COMMIT;