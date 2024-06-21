-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
SELECT JSON_OBJECT('city'     : city,
                   'province' : state_province ABSENT ON NULL) 
  FROM hr.locations
  WHERE city LIKE 'S%';