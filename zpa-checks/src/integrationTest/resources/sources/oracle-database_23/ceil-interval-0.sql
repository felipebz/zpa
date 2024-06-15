-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ceil-interval.html
SELECT CEIL(INTERVAL '+123-5' YEAR(3) TO MONTH) AS year_ceil;