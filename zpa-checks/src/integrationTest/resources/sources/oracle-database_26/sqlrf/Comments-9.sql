-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/Comments.html
SELECT /*+ DRIVING_SITE(departments) */ * 
  FROM employees, departments@rsite 
  WHERE employees.department_id = departments.department_id;