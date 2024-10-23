-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-function-json_object.html
SELECT json_object('id'          : employee_id,
                   'name'        : first_name || ' ' || last_name,
                   'contactInfo' : json_object('mail'  : email,
                                               'phone' : phone_number),
                   'hireDate'    : hire_date,
                   'pay'         : salary
                   RETURNING JSON) 
  FROM hr.employees
  WHERE salary > 15000;