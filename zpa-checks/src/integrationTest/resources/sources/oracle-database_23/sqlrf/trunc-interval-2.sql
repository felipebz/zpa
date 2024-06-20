-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/trunc-interval.html
SELECT TRUNC(INTERVAL '+4 12:42:10.222' DAY(2) TO SECOND(3), 'DD') AS day_trunc;