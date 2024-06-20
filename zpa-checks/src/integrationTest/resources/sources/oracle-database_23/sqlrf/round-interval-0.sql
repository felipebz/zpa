-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/round-interval.html
SELECT ROUND(INTERVAL '+123-06' YEAR(3) TO MONTH) AS year_round;