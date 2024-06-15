-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/APPROX_COUNT_DISTINCT.html
SELECT prod_id, APPROX_COUNT_DISTINCT(cust_id) AS "Number of Customers"
  FROM sales
  GROUP BY prod_id
  ORDER BY prod_id;