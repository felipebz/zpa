-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE employees a 
    SET department_id = 
        (SELECT department_id 
            FROM departments 
            WHERE location_id = '2100'), 
        (salary, commission_pct) = 
        (SELECT 1.1*AVG(salary), 1.5*AVG(commission_pct) 
          FROM employees b 
          WHERE a.department_id = b.department_id) 
    WHERE department_id IN 
        (SELECT department_id 
          FROM departments
          WHERE location_id = 2900 
              OR location_id = 2700);