-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/round-interval.html
SELECT ROUND(INTERVAL '-999999999-11' YEAR(9) TO MONTH, 'YEAR')AS year_round;