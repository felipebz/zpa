-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ceil-interval.html
SELECT CEIL(INTERVAL '+999999999-11' YEAR(9) TO MONTH, 'YEAR') AS year_ceil;

ORA-01873: the leading precision of the interval is too small