-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operation-handlers.html
SELECT json_transform('{"a":"dog"}',
                      APPEND '$.a' = 'cat'
                      REPLACE ON MISMATCH);