-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/sql-json-path-expression-item-methods.html
SELECT json_value('[ 19, "Oracle", {"a":1}, [1,2,3] ]', '$.type()');