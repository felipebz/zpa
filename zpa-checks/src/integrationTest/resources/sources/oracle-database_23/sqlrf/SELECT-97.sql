-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT d.department_name, e2.last_name, e2.first_name, e2.salary, e2.email
  FROM hr.departments d, 
     LATERAL (SELECT * FROM 
                       (SELECT * FROM hr.employees e
                        WHERE e.department_id = d.department_id
                        ORDER BY e.salary DESC, e.employee_id ASC)
               WHERE ROWNUM = 1) e2;