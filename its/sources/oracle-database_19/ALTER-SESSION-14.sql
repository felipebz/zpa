UPDATE jobs@local SET min_salary = 3000
   WHERE job_id = 'SH_CLERK';

COMMIT; 

ALTER SESSION
   CLOSE DATABASE LINK local;