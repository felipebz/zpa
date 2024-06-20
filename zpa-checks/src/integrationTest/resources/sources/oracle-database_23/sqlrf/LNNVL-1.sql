-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/LNNVL.html
SELECT COUNT(*)
  FROM employees
  WHERE LNNVL(commission_pct >= .2);