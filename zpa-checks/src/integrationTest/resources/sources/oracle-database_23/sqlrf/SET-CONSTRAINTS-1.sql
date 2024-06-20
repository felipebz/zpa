-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SET-CONSTRAINTS.html
SET CONSTRAINTS emp_job_nn, emp_salary_min,
   hr.jhist_dept_fk@remote DEFERRED;