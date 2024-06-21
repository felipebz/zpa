-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
SELECT cache_id, COUNT(cache_key) AS uniq_args
FROM GV$RESULT_CACHE_OBJECTS
WHERE type = 'Result'
GROUP BY cache_id
ORDER BY uniq_args DESC;