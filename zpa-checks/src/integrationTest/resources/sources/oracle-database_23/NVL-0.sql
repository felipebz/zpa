-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NVL.html
SELECT last_name, NVL(TO_CHAR(commission_pct), 'Not Applicable') commission
  FROM employees
  WHERE last_name LIKE 'B%'
  ORDER BY last_name;