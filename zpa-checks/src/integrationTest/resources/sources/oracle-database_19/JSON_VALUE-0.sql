-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('{a:100}', '$.a') AS value
  FROM DUAL;