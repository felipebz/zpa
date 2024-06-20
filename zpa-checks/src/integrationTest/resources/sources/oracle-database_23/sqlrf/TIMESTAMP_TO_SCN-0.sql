-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TIMESTAMP_TO_SCN.html
INSERT INTO orders (order_id, order_date, customer_id, order_total)
   VALUES (5000, SYSTIMESTAMP, 188, 2345);
COMMIT;