-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('{a:{b:100}}', '$.a.b') AS value
  FROM DUAL;