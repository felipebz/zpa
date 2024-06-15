-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_QUERY.html
SELECT JSON_QUERY('[{"a":100},{"b":200},{"c":300}]', '$[3]'
       EMPTY ON ERROR) AS value
  FROM DUAL;