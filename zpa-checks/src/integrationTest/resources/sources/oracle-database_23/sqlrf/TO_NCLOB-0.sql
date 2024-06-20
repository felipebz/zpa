-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TO_NCLOB.html
INSERT INTO print_media (product_id, ad_id, ad_fltextn)
   VALUES (3502, 31001, 
      TO_NCLOB('Placeholder for new product description'));