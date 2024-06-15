-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/STDDEV_POP.html
SELECT department_id, last_name, salary, 
   STDDEV_POP(salary) OVER (PARTITION BY department_id) AS pop_std
   FROM employees
   ORDER BY department_id, last_name, salary, pop_std;