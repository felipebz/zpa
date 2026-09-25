-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION employee_in_every_dept
CHECK (
  ALL (
    SELECT d.department_id 
    FROM departments d
  ) d
  SATISFY (
    EXISTS (
      SELECT 'an employee' FROM employees e
      WHERE e.department_id = d.department_id
      AND e.department_id IS NOT NULL
    )
  )
)
DEFERRABLE INITIALLY DEFERRED;