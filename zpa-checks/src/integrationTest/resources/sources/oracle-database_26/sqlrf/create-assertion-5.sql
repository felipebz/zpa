-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION staff_earn_less_than_manager
CHECK (
  ALL (
    SELECT staff.salary staff_salary,
           mgr.salary manager_salary
    FROM hr.employees staff,
         hr.employees mgr
    WHERE staff.manager_id = mgr.employee_id
    AND staff.manager_id IS NOT NULL 
  ) staff
  SATISFY (
    staff_salary < manager_salary
  )
) NOVALIDATE;