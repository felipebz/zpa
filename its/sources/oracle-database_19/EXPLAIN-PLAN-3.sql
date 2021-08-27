-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/EXPLAIN-PLAN.html
EXPLAIN PLAN FOR
  SELECT * FROM sales 
     WHERE time_id BETWEEN :h AND '01-OCT-2000';