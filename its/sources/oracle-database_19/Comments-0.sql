-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Comments.html
SELECT last_name, employee_id, salary + NVL(commission_pct, 0), 
       job_id, e.department_id
  /* Select all employees whose compensation is
  greater than that of Pataballa.*/
  FROM employees e, departments d
  /*The DEPARTMENTS table is used to get the department name.*/
  WHERE e.department_id = d.department_id
    AND salary + NVL(commission_pct,0) >   /* Subquery:       */
      (SELECT salary + NVL(commission_pct,0)
        /* total compensation is salary + commission_pct */
        FROM employees 
        WHERE last_name = 'Pataballa')
  ORDER BY last_name, employee_id;

SELECT last_name,                                   -- select the name
       employee_id                                  -- employee id
       salary + NVL(commission_pct, 0),             -- total compensation
       job_id,                                      -- job
       e.department_id                              -- and department
  FROM employees e,                                 -- of all employees
       departments d
  WHERE e.department_id = d.department_id
    AND salary + NVL(commission_pct, 0) >           -- whose compensation 
                                                    -- is greater than
        (SELECT salary + NVL(commission_pct,0)      -- the compensation
          FROM employees 
          WHERE last_name = 'Pataballa')            -- of Pataballa
  ORDER BY last_name                                -- and order by last name
           employee_id                              -- and employee id.
;