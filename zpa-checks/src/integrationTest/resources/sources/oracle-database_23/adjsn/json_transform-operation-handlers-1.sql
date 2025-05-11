-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operation-handlers.html
SELECT json_transform('{"a":null, "b":[ 1,2,3 ]}',
                      APPEND '$.b' = PATH '$.x'
                      ERROR ON EMPTY);