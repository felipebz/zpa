-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_array.html
SELECT JSON { 'title'       VALUE job_title, 
              'salaryRange' VALUE [ min_salary, max_salary ] }
  FROM jobs;