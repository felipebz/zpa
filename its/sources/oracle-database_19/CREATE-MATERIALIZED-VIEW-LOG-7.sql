CREATE MATERIALIZED VIEW products_mv 
   REFRESH FAST ON COMMIT
   AS SELECT SUM(list_price - min_price), category_id
         FROM product_information 
         GROUP BY category_id;