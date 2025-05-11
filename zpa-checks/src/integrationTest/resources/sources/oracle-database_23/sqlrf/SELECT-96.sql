-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT department_name, 
   (SELECT last_name FROM 
     (SELECT last_name FROM hr.employees e
       WHERE e.department_id = d.department_id
       ORDER BY e.salary DESC, e.employee_id ASC) 
     WHERE ROWNUM = 1) highest_paid
  FROM hr.departments d;