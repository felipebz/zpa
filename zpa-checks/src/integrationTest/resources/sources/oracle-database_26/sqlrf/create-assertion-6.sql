-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/create-assertion.html
CREATE ASSERTION hr.salary_within_job_limit
CHECK (
  ALL (
    SELECT job_id, salary
    FROM hr.employees
  ) emp
  SATISFY (
    EXISTS (
      SELECT 'salary within pay band'
      FROM hr.jobs job
      WHERE emp.job_id = job.job_id
      AND emp.salary BETWEEN job.min_salary AND job.max_salary
     )
  )
)
DISABLE;