-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_object.html
SELECT json_object(* RETURNING JSON)
  FROM hr.employees
  WHERE salary > 15000;