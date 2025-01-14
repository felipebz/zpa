-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_TIMESTAMP_TZ.html
SELECT order_id, line_item_id,
  CAST(NULL AS TIMESTAMP WITH LOCAL TIME ZONE) order_date
  FROM order_items
UNION
SELECT order_id, to_number(null), order_date
  FROM orders;