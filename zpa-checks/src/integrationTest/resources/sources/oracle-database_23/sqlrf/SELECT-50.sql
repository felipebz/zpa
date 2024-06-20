-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
UPDATE employees SET salary =      
   (SELECT salary FROM employees
   AS OF TIMESTAMP (SYSTIMESTAMP - INTERVAL '2' MINUTE)
   WHERE last_name = 'Chung')
   WHERE last_name = 'Chung';
SELECT salary FROM employees
   WHERE last_name = 'Chung';