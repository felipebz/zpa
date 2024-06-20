-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CONCAT.html
SELECT CONCAT( last_name, '''s job category is ', job_id) "Job" 
  FROM employees 
  WHERE employee_id = 152;