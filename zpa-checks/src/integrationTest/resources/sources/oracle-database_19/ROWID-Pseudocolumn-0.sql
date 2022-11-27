-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ROWID-Pseudocolumn.html
SELECT ROWID, last_name
  FROM employees
  WHERE department_id = 20;