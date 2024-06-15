-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/boolean_or_agg.html
SELECT BOOLEAN_OR_AGG(c2 OR c2)
    FROM t
    WHERE c2 IS NOT TRUE OR c2 IS NULL;