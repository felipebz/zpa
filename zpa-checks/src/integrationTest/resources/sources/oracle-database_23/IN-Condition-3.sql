-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/IN-Condition.html
SELECT * FROM employees
  WHERE salary NOT IN
  (SELECT salary 
   FROM employees
  WHERE department_id = 30)
  ORDER BY employee_id;
SELECT * FROM employees
  WHERE job_id NOT IN
  ('PU_CLERK', 'SH_CLERK')
  ORDER BY employee_id;