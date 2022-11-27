-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('[{a:100}, {b:200}, {c:300}]', '$[*].c') AS value
  FROM DUAL;