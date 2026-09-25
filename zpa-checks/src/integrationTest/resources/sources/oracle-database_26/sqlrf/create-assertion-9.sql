-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION employee_without_criminal_record_in_every_dept
CHECK (
  NOT EXISTS (
    SELECT 'a department'
    FROM departments d
    WHERE NOT EXISTS (
      SELECT 'an employee' FROM employees e
      WHERE e.department_id = d.department_id
      AND e.department_id IS NOT NULL
      AND NOT EXISTS (
        SELECT 'a criminal record'
        FROM criminal_records cr
        WHERE cr.employee_id = e.employee_id
      )
    )
  )
)
DEFERRABLE INITIALLY DEFERRED;