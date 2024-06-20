-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/round-interval.html
SELECT ROUND(INTERVAL '+99-11' YEAR(2) TO MONTH, 'YEAR') AS year_round;