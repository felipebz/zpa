-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_OBJECT.html
SELECT JSON_OBJECT(
'name' : first_name || ' ' || last_name,
'email' : email,
'phone' : phone_number,
'hire_date' : hire_date
)
FROM employees
WHERE employee_id = 140;