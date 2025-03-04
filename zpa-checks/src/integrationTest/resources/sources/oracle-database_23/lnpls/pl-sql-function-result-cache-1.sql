-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pl-sql-function-result-cache.html
SELECT SCAN_COUNT, COUNT(CACHE_KEY)
FROM V$RESULT_CACHE_OBJECTS
WHERE NAMESPACE = 'PLSQL'
GROUP BY SCAN_COUNT;