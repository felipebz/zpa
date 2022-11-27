-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/DECODE.html
SELECT product_id,
       DECODE (warehouse_id, 1, 'Southlake', 
                             2, 'San Francisco', 
                             3, 'New Jersey', 
                             4, 'Seattle',
                                'Non domestic') "Location" 
  FROM inventories
  WHERE product_id < 1775
  ORDER BY product_id, "Location";