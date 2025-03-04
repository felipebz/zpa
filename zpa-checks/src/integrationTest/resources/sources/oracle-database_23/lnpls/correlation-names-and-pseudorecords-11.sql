-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/correlation-names-and-pseudorecords.html
UPDATE employees
SET salary = salary * 1.05
WHERE department_id IN (10, 20, 90);