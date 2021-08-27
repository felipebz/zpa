-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/TO_NCHAR-number.html
SELECT TO_NCHAR(customer_id) "NCHAR_Customer_ID"  FROM orders 
   WHERE order_status > 9
   ORDER BY "NCHAR_Customer_ID";