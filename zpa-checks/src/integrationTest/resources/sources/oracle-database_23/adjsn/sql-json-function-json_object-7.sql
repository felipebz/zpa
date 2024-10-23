-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_object.html
SELECT JSON { last_name,
              'contactInfo' : JSON { email, phone_number },
              hire_date,
              salary}
  FROM hr.employees
  WHERE employee_id = 101;