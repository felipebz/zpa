-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_QUERY.html
SELECT JSON_QUERY('[0,1,2,3,4]', '$') AS value
  FROM DUAL;