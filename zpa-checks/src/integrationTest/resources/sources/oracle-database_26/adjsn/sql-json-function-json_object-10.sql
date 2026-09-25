-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/sql-json-function-json_object.html
SELECT JSON { * }
  FROM hr.employees
  WHERE salary > 15000;