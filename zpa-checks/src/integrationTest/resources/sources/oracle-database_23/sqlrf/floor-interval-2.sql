-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/floor-interval.html
SELECT FLOOR(INTERVAL '+4 12:42:10.222' DAY(2) TO SECOND(3), 'DD') as year_floor;