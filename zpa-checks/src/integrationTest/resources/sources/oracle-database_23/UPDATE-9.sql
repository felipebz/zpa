-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE hr.employees e
    SET e.salary = j.max_salary
    FROM hr.jobs j    
    WHERE e.job_id = j.job_id;