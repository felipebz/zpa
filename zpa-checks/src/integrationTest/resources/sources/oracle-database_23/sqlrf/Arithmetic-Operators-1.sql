-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Arithmetic-Operators.html
SELECT hire_date 
  FROM employees
  WHERE SYSDATE - hire_date > 365
  ORDER BY hire_date;