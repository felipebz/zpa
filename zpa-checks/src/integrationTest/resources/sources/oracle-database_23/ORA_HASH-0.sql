-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ORA_HASH.html
SELECT SUM(amount_sold)
  FROM sales
  WHERE ORA_HASH(CONCAT(cust_id, prod_id), 99, 5) = 0;