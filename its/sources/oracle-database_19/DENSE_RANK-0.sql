-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/DENSE_RANK.html
SELECT DENSE_RANK(15500, .05) WITHIN GROUP 
  (ORDER BY salary DESC, commission_pct) "Dense Rank" 
  FROM employees;