SELECT last_name, salary, VARIANCE(salary) 
      OVER (ORDER BY hire_date) "Variance"
   FROM employees 
   WHERE department_id = 30
   ORDER BY last_name, salary, "Variance"; 