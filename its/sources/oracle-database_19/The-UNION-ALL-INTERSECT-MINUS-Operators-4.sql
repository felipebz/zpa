SELECT product_id FROM order_items
UNION
SELECT product_id FROM inventories
ORDER BY product_id;

SELECT location_id  FROM locations 
UNION ALL 
SELECT location_id  FROM departments
ORDER BY location_id;