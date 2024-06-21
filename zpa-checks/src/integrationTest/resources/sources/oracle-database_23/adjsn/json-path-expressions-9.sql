-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json-path-expressions.html
SELECT json_value('[ 19, "Oracle", {"a":1}, [1,2,3] ]', '$.type()')
  FROM dual;