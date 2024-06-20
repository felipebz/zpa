-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT time_id, product, LAST_VALUE(quantity IGNORE NULLS) 
   OVER (PARTITION BY product ORDER BY time_id) quantity 
   FROM ( SELECT times.time_id, product, quantity 
             FROM inventory PARTITION BY  (product) 
                RIGHT OUTER JOIN times ON (times.time_id = inventory.time_id) 
   WHERE times.time_id BETWEEN TO_DATE('01/04/01', 'DD/MM/YY') 
      AND TO_DATE('06/04/01', 'DD/MM/YY')) 
   ORDER BY  2,1;