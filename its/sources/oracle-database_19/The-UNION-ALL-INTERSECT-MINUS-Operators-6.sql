-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/The-UNION-ALL-INTERSECT-MINUS-Operators.html
SELECT product_id FROM inventories
MINUS
SELECT product_id FROM order_items
ORDER BY product_id;