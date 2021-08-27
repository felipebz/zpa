-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SET.html
SELECT customer_id, SET(cust_address_ntab) address
  FROM customers_demo
  ORDER BY customer_id;