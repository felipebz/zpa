-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-set.html
SELECT json_transform(JSON('{"alpha" : {"a":1, "b":2}}'),
         SET '$.alpha.z' = 26)