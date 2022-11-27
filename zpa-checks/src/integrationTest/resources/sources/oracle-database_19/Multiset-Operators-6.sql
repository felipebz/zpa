-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Multiset-Operators.html
SELECT customer_id, cust_address_ntab
  MULTISET UNION cust_address2_ntab multiset_union
  FROM customers_demo
  ORDER BY customer_id;