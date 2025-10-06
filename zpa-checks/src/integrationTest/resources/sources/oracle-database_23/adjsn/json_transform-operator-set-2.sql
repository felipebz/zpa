-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-operator-set.html
SELECT json_transform(JSON('{"alpha" : [ 1,2 ]}'),
         SET '$.alpha.z' = 26)