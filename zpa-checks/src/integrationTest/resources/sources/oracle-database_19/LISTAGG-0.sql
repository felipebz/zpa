-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/LISTAGG.html
SELECT LISTAGG(last_name, '; ')
         WITHIN GROUP (ORDER BY hire_date, last_name) "Emp_list",
       MIN(hire_date) "Earliest"
  FROM employees
  WHERE department_id = 30;