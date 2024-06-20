-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SCN_TO_TIMESTAMP.html
SELECT salary FROM employees WHERE employee_id = 188;
UPDATE employees SET salary = salary*10 WHERE employee_id = 188;
COMMIT;
SELECT salary FROM employees WHERE employee_id = 188;