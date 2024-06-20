-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/NULLIF.html
SELECT e.last_name, NULLIF(j.job_id, e.job_id) "Old Job ID"
  FROM employees e, job_history j
  WHERE e.employee_id = j.employee_id
  ORDER BY last_name, "Old Job ID";