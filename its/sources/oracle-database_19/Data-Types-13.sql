-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html
SELECT order_id, order_date + INTERVAL '30' DAY AS "Due Date"
  FROM orders
  ORDER BY order_id, "Due Date";