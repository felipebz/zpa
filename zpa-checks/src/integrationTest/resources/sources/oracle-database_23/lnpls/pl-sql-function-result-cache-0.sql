-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pl-sql-function-result-cache.html
SELECT rc1.NAME, rc2.STATUS, rc3.STATUS, rc2.BLOCK_COUNT
FROM V$RESULT_CACHE_OBJECTS rc1, V$RESULT_CACHE_OBJECTS rc2
WHERE rc1.TYPE = 'Result'
AND rc2.TYPE = 'Temp'
AND rc1.CACHE_KEY = rc2.CACHE_KEY;