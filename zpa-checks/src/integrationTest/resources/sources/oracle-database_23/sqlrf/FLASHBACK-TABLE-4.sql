-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/FLASHBACK-TABLE.html
UPDATE employees_test
  SET salary = salary * 1.1
  WHERE salary < 2500;

5 rows updated.
COMMIT;