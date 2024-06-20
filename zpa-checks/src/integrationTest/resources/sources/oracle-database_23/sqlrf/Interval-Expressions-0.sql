-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Interval-Expressions.html
SELECT (SYSTIMESTAMP - order_date) DAY(9) TO SECOND FROM orders
   WHERE order_id = 2458;