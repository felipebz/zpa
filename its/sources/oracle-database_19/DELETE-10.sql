-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/DELETE.html
DELETE product_price_history pp 
WHERE  (product_id, currency_code, effective_from_date) 
   IN (SELECT product_id, currency_code, Max(effective_from_date) 
       FROM   product_price_history 
       GROUP BY product_id, currency_code);