-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ORA_ROWSCN-Pseudocolumn.html
SELECT ORA_ROWSCN, last_name
  FROM employees
  WHERE employee_id = 188;
SELECT SCN_TO_TIMESTAMP(ORA_ROWSCN), last_name
  FROM employees
  WHERE employee_id = 188;