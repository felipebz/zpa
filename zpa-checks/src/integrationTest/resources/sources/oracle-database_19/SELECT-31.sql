-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/SELECT.html
UPDATE employees SET salary =      
   (SELECT salary FROM employees
   AS OF TIMESTAMP (SYSTIMESTAMP - INTERVAL '2' MINUTE)
   WHERE last_name = 'Chung')
   WHERE last_name = 'Chung';
1 row updated.

SELECT salary FROM employees
   WHERE last_name = 'Chung';