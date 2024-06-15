-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COVAR_POP.html
SELECT product_id, supplier_id,
       COVAR_POP(list_price, min_price) 
         OVER (ORDER BY product_id, supplier_id)
         AS CUM_COVP,
       COVAR_SAMP(list_price, min_price)
         OVER (ORDER BY product_id, supplier_id)
         AS CUM_COVS 
  FROM product_information p
  WHERE category_id = 29
  ORDER BY product_id, supplier_id;