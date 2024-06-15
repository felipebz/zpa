-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
SELECT job_name,
       SUM( cpu_used )
  FROM DBA_SCHEDULER_JOB_RUN_DETAILS
  GROUP BY job_name
  HAVING SUM ( cpu_used ) > interval '5' minute;