-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operation-handlers.html
SELECT json_transform('{"a":"notAnObject"}',
                      MERGE '$.a' = PATH '$var'
                      IGNORE ON MISMATCH
                      PASSING JSON('{"b":2}') AS "var");