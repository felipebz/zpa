-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_QUERY.html
SELECT JSON_QUERY('{a:100, b:200, c:300}', '$.a' WITH WRAPPER) AS value
  FROM DUAL;