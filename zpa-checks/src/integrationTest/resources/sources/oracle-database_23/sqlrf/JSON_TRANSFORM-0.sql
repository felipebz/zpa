-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_TRANSFORM.html
UPDATE t SET jcol = JSON_TRANSFORM(jcol, SET '$.lastUpdated' = SYSTIMESTAMP)