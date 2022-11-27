-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Logical-Conditions.html
SELECT * FROM employees
WHERE hire_date < TO_DATE('01-JAN-2004', 'DD-MON-YYYY')
  AND salary > 2500
  ORDER BY employee_id;