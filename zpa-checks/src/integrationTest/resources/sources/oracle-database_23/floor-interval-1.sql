-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/floor-interval.html
SELECT FLOOR(INTERVAL '+99-11' YEAR(2) TO MONTH, 'YEAR') as year_floor;