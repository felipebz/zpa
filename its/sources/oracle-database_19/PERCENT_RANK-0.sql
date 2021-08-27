-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/PERCENT_RANK.html
SELECT PERCENT_RANK(15000, .05) WITHIN GROUP
       (ORDER BY salary, commission_pct) "Percent-Rank" 
  FROM employees;