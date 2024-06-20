-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NVL2.html
SELECT last_name, salary,
       NVL2(commission_pct, salary + (salary * commission_pct), salary) income
  FROM employees
  WHERE last_name like 'B%'
  ORDER BY last_name;