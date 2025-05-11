-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operation-handlers.html
SELECT json_transform('{"created":"2025-04-09T22:07:06"}',
                      SET '$.created' = SYSDATE
                      IGNORE ON EXISTING);