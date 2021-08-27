-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/LISTAGG.html
SELECT department_id "Dept.",
       LISTAGG(last_name, '; ') WITHIN GROUP (ORDER BY hire_date) "Employees"
  FROM employees
  GROUP BY department_id
  ORDER BY department_id;