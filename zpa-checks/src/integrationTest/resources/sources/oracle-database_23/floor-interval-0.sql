-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/floor-interval.html
SELECT FLOOR(INTERVAL '+123-5' YEAR(3) TO MONTH) as year_floor;