-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/VARIANCE.html
SELECT last_name, salary, VARIANCE(salary) 
      OVER (ORDER BY hire_date) "Variance"
   FROM employees 
   WHERE department_id = 30
   ORDER BY last_name, salary, "Variance"; 