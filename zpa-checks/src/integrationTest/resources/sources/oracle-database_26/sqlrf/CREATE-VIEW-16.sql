-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/CREATE-VIEW.html
  CREATE OR REPLACE VIEW high_wage_employees (
  employee_id ANNOTATIONS (identity, display 'Employee ID'),
  first_name ANNOTATIONS (display 'Employee Name'),
  salary ANNOTATIONS (display 'Emp Salary') )
  ANNOTATIONS (Title 'High Wage Employee View') AS
  SELECT employee_id, first_name, salary
  FROM employees
  WHERE salary > 100000;