CREATE VIEW emp_view AS 
   SELECT last_name, salary*12 annual_salary
   FROM employees 
   WHERE department_id = 20;