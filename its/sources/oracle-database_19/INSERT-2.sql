-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/INSERT.html
INSERT INTO employees (employee_id, last_name, email, 
      hire_date, job_id, salary, commission_pct) 
   VALUES (207, 'Gregory', 'pgregory@example.com', 
      sysdate, 'PU_CLERK', 1.2E3, NULL);