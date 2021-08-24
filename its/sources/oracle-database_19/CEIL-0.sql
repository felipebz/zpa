SELECT order_total, CEIL(order_total)
  FROM orders
  WHERE order_id = 2434;