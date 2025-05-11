-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-rename.html
SELECT json_transform('{"abc":1}',
                      RENAME '$.abc' = 'xyz',
                      SET '$.xyz' = 2)