-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DELETE.html
DECLARE 
  currency product_price_history.currency_code%TYPE; 
BEGIN 
  DELETE product_price_history 
  WHERE  product_id = 2 
  AND    effective_to_date IS NULL 
  returning currency_code INTO currency; 
      
  dbms_output.Put_line(currency); 
END;

USD