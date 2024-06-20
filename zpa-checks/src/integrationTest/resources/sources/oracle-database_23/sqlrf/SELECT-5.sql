-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/SELECT.html
WITH X(foo, bar, baz) AS (
         VALUES (0, 1, 2), (3, 4, 5), (6, 7, 8) ) SELECT * FROM X;