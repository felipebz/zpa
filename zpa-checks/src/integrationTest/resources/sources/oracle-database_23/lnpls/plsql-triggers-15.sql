-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
SELECT employee_id, last_name, job_id, salary
FROM employees
WHERE department_id = 10;