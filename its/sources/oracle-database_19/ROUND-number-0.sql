-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/ROUND-number.html
ROUND(n, integer) = FLOOR(n * POWER(10, integer) + 0.5) * POWER(10, -integer)