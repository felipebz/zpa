-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CUME_DIST.html
SELECT CUME_DIST(15500, .05) WITHIN GROUP
  (ORDER BY salary, commission_pct) "Cume-Dist of 15500" 
  FROM employees;