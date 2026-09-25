-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION IF NOT EXISTS company_must_have_a_president
CHECK (
  EXISTS (
    SELECT 'a president'
    FROM employees
    WHERE job_id = 'AD_PRES'
   )
);