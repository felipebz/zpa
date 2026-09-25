-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+ USE_MERGE(employees departments) */ * 
  FROM employees, departments 
  WHERE employees.department_id = departments.department_id;