-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SCN_TO_TIMESTAMP.html
SELECT SCN_TO_TIMESTAMP(ORA_ROWSCN) FROM employees
   WHERE employee_id = 188;
FLASHBACK TABLE employees TO TIMESTAMP
   TO_TIMESTAMP('28-AUG-03 01.00.00.000000000 PM');
SELECT salary FROM employees WHERE employee_id = 188;