-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ANY_VALUE.html
SELECT c.cust_id, ANY_VALUE(cust_last_name), SUM(amount_sold)
  FROM customers c, sales s
  WHERE s.cust_id = c.cust_id
  GROUP BY c.cust_id;