SELECT TO_NCHAR(customer_id) "NCHAR_Customer_ID"  FROM orders 
   WHERE order_status > 9
   ORDER BY "NCHAR_Customer_ID";