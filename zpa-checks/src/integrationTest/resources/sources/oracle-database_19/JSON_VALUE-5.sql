-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('{a:[5, 10, 15, 20]}', '$.a[2]') AS value
  FROM DUAL;