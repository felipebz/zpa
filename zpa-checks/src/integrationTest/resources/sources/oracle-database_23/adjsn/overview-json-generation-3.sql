-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/overview-json-generation.html
SELECT json_objectagg(department_name VALUE department_id
                      RETURNING JSON)
  FROM departments;