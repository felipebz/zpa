-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/JSON_QUERY.html
SELECT JSON_QUERY('[0,1,2,3,4]', '$[3]' WITH WRAPPER) AS value
  FROM DUAL;