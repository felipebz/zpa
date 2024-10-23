-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_objectagg.html
SELECT json_objectagg(department_name VALUE department_id)
  FROM departments;