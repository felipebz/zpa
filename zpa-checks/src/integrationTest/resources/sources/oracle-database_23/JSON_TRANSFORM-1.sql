-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_TRANSFORM.html
SELECT JSON_TRANSFORM (jcol, REMOVE '$.ssn') FROM t WHERE …