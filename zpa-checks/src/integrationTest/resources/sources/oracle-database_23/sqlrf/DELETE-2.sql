-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DELETE.html
DELETE FROM employees
   WHERE job_id = 'SA_REP'
   AND commission_pct < .2;