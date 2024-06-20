-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('{a:100}', '$.a' RETURNING NUMBER) AS value
  FROM DUAL;