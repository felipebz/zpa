CREATE TABLE temporary 
   (employee_id, start_date, end_date, job_id, dept_id) 
AS SELECT 
     employee_id, start_date, end_date, job_id, department_id
FROM job_history; 

DROP TABLE job_history; 

RENAME temporary TO job_history;