-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/correlation-names-and-pseudorecords.html
SELECT last_name, department_id, salary, job_id
FROM employees
WHERE department_id IN (10, 20, 90)
ORDER BY department_id, last_name;