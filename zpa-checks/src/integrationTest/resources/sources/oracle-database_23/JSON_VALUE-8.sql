-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_VALUE.html
SELECT JSON_VALUE('{firstname:"John"}', '$.lastname') AS "Last Name"
  FROM DUAL;