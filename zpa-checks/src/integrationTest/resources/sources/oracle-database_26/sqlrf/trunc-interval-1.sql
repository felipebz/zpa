-- https://docs.oracle.com/en/database/oracle/oracle-database/26/sqlrf/trunc-interval.html
SELECT TRUNC(INTERVAL '+99-11' YEAR(2) TO MONTH, 'YEAR') AS year_trunc;