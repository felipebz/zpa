-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/STDDEV.html
SELECT last_name, salary, 
   STDDEV(salary) OVER (ORDER BY hire_date) "StdDev"
   FROM employees  
   WHERE department_id = 30
   ORDER BY last_name, salary, "StdDev"; 