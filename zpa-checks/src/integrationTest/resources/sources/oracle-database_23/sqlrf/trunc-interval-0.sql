-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/trunc-interval.html
SELECT TRUNC(INTERVAL '+123-06' YEAR(3) TO MONTH) AS year_trunc;