-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/UPDATE.html
UPDATE employees
   SET commission_pct = NULL
   WHERE job_id = 'SH_CLERK';