-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/boolean_and_agg.html
SELECT BOOLEAN_AND_AGG(c2)
    FROM t
    WHERE c2 IS FALSE;