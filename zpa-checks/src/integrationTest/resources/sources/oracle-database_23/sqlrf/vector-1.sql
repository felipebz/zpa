-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector.html
SELECT VECTOR('[34.6, 77.8]');
SELECT VECTOR('[34.6, 77.8]', 2, FLOAT32);
SELECT VECTOR('[34.6, 77.8, -89.34]', 3, FLOAT32);