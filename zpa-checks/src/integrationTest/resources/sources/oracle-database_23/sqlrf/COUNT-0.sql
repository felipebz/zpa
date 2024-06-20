-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COUNT.html
SELECT COUNT(*) "Total"
  FROM employees;
SELECT COUNT(*) "Allstars"
  FROM employees
  WHERE commission_pct > 0;
SELECT COUNT(commission_pct) "Count"
  FROM employees;
SELECT COUNT(DISTINCT manager_id) "Managers"
  FROM employees;