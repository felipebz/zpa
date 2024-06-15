-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('[0, 1, 2, 3]', '$[0]') AS value
  FROM DUAL;