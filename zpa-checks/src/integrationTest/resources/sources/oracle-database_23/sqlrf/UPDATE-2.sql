-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE employees SET 
    job_id = 'SA_MAN', salary = salary + 1000, department_id = 120 
    WHERE first_name||' '||last_name = 'Douglas Grant';