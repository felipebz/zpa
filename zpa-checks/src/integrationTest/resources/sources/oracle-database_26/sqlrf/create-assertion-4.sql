-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION no_overlapping_job_history
CHECK (
  ALL (
    SELECT jh1.start_date first_start, jh1.end_date first_end,
           jh2.start_date next_start, jh2.end_date next_end
    FROM job_history jh1, job_history jh2
    WHERE jh1.employee_id = jh2.employee_id
    AND jh1.ROWID <> jh2.ROWID
  ) jh
  SATISFY (
      jh.first_end <= jh.next_start
   OR jh.first_start >= jh.next_end
 )
);