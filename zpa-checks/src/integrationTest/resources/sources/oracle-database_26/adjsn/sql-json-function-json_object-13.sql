-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/sql-json-function-json_object.html
SELECT JSON {'city'     : city,
             'province' : state_province ABSENT ON NULL} 
  FROM hr.locations
  WHERE city LIKE 'S%';