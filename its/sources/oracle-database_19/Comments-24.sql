-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT /*+ NO_INDEX(employees emp_empid) */ employee_id 
  FROM employees 
  WHERE employee_id > 200;