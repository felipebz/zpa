-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/overview-json-generation.html
SELECT JSON(json_objectagg(department_name VALUE department_id))
  FROM departments;