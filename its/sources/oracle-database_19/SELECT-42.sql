-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
SELECT last_name, department_id, salary 
   FROM employees 
   ORDER BY 2 ASC, 3 DESC, 1;