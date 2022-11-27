-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Multiset-Conditions.html
SELECT customer_id, cust_address_ntab
  FROM customers_demo
  WHERE cust_address_ntab SUBMULTISET OF cust_address2_ntab
  ORDER BY customer_id;