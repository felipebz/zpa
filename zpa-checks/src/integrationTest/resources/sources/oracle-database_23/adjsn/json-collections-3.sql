-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-collections.html
CREATE JSON COLLECTION VIEW empview AS
  SELECT JSON {'_id'         : employee_id,
               last_name,             
               'contactInfo' : {email, phone_number},
               hire_date,
               salary}
    FROM hr.employees;