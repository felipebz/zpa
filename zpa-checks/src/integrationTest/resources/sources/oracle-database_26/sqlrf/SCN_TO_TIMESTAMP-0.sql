-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/SCN_TO_TIMESTAMP.html
SELECT SCN_TO_TIMESTAMP(ORA_ROWSCN) FROM employees
   WHERE employee_id = 188;