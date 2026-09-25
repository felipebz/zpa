-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION no_overlapping_job_history
CHECK (
  NOT EXISTS (
    SELECT 'overlapping job history'  
    FROM job_history jh1,
         job_history jh2
    WHERE jh1.employee_id = jh2.employee_id 
    AND jh1.ROWID <> jh2.ROWID
    AND jh1.start_date < jh2.end_date
    AND jh1.end_date > jh2.start_date
   )
);