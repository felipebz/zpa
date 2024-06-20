-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT department_id, manager_id 
   FROM employees 
   GROUP BY department_id, manager_id HAVING (department_id, manager_id) IN
   (SELECT department_id, manager_id FROM employees x 
      WHERE x.department_id = employees.department_id)
   ORDER BY department_id;