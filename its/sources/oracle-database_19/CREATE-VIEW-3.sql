CREATE VIEW clerk AS
   SELECT employee_id, last_name, department_id, job_id 
   FROM employees
   WHERE job_id = 'PU_CLERK' 
      or job_id = 'SH_CLERK' 
      or job_id = 'ST_CLERK';