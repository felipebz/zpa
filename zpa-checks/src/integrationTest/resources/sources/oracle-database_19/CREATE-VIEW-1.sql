-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-VIEW.html
CREATE EDITIONING VIEW ed_orders_view (o_id, o_date, o_status)
  AS SELECT order_id, order_date, order_status FROM orders
  WITH READ ONLY;