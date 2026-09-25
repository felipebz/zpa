-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/LNNVL.html
SELECT COUNT(*)
  FROM employees
  WHERE commission_pct < .2;