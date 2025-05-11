-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{"a":1}',
             SET '$var1' = 2,
             SET '$var2' = PATH '$.a',
             SET '$.b'   = PATH '$var1 + $var2 + $var3'
             PASSING 5 AS "var3");