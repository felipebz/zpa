-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
SELECT object_no, SUM(invalidations) AS num_invals
FROM GV$RESULT_CACHE_OBJECTS
WHERE type = 'Dependency'
GROUP BY object_no
ORDER BY num_invals DESC;