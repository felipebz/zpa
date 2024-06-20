-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/RANK.html
SELECT RANK(15500, .05) WITHIN GROUP
   (ORDER BY salary, commission_pct) "Rank"
   FROM employees;