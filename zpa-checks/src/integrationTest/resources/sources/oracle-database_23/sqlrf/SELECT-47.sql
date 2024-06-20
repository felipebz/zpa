-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
SELECT salary FROM employees
   WHERE last_name = 'Chung';
UPDATE employees SET salary = 4000
   WHERE last_name = 'Chung';
SELECT salary FROM employees
   WHERE last_name = 'Chung';