-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/WIDTH_BUCKET.html
SELECT customer_id, cust_last_name, credit_limit, 
   WIDTH_BUCKET(credit_limit, 100, 5000, 10) "Credit Group"
   FROM customers WHERE nls_territory = 'SWITZERLAND'
   ORDER BY "Credit Group", customer_id, cust_last_name, credit_limit;