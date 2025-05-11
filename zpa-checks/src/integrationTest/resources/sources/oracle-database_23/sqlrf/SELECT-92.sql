-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT department_name FROM hr.departments d
  WHERE NOT EXISTS (SELECT asdf FROM hr.employees e
                  WHERE e.department_id = d.department_id
                  AND e.salary >= 10000)
  ORDER BY department_name;