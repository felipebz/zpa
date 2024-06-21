-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_object('title'       VALUE job_title, 
                   'salaryRange' VALUE json_array(min_salary, max_salary)
                   RETURNING JSON)
  FROM jobs;