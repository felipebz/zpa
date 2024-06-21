-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT json_object(* RETURNING JSON)
  FROM hr.employees
  WHERE salary > 15000;