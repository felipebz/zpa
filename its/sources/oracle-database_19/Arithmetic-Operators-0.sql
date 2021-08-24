SELECT *
  FROM order_items
  WHERE quantity = -1
  ORDER BY order_id, 
    line_item_id, product_id;

SELECT *
  FROM employees
  WHERE -salary < 0
  ORDER BY employee_id;