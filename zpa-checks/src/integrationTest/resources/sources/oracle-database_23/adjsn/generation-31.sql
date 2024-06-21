-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT JSON { 'title'       VALUE job_title, 
              'salaryRange' VALUE [ min_salary, max_salary ] }
  FROM jobs;