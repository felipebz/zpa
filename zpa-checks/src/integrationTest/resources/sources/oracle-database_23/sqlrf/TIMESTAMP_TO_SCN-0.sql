-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TIMESTAMP_TO_SCN.html
INSERT INTO orders (order_id, order_date, customer_id, order_total)
   VALUES (5000, SYSTIMESTAMP, 188, 2345);
1 row created.

COMMIT;
Commit complete.

SELECT TIMESTAMP_TO_SCN(order_date) FROM orders
   WHERE order_id = 5000;