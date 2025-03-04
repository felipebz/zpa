-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/correlation-names-and-pseudorecords.html
UPDATE employees
SET salary = salary + 1000.0
WHERE Department_id = 20;