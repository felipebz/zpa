-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CEIL.html
SELECT order_total, CEIL(order_total)
  FROM orders
  WHERE order_id = 2434;