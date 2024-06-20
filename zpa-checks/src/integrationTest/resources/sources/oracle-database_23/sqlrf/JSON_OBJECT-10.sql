-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_OBJECT.html
SELECT JSON_OBJECT(
'first_name' VALUE first_name,
'last_name' VALUE last_name,
'email' VALUE email,
'hire_date' VALUE hire_date
)
FROM employees
WHERE employee_id = 140;