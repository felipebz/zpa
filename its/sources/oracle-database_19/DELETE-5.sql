-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/DELETE.html
DELETE FROM employees
   WHERE job_id = 'SA_REP' 
   AND hire_date + TO_YMINTERVAL('01-00') < SYSDATE 
   RETURNING salary INTO :bnd1;