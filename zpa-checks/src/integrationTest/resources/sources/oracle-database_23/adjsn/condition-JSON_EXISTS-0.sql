-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/condition-JSON_EXISTS.html
SELECT json_exists('{a : null}', '$.a') FROM DUAL;