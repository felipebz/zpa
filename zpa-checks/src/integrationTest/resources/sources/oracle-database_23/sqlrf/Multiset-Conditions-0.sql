-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Multiset-Conditions.html
SELECT customer_id, cust_address_ntab
  FROM customers_demo
  WHERE cust_address_ntab IS A SET
  ORDER BY customer_id;