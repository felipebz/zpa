-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Multiset-Operators.html
SELECT customer_id, cust_address_ntab
  MULTISET EXCEPT DISTINCT cust_address2_ntab multiset_except
  FROM customers_demo
  ORDER BY customer_id;