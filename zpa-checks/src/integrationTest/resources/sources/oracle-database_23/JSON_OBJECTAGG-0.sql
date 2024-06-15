-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_OBJECTAGG.html
SELECT JSON_OBJECTAGG(KEY department_name VALUE department_id) "Department Numbers"
  FROM departments
  WHERE department_id <= 30;