-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/json_transform-right-hand-side-rhs-path-expressions.html
SELECT json_transform('{"salary":1000, "commission":150}',
                      SET '$.bonus' = PATH '$.salary * $bonusFactor',
                      SET '$.compensation' = PATH '($.salary + $.bonus)
                                                   + $.commission'
                      PASSING 0.05 AS "bonusFactor");