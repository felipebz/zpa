-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/to_vector.html
SELECT TO_VECTOR('[34.6, 77.8]');
SELECT TO_VECTOR('[34.6, 77.8]', 2, FLOAT32);

SELECT TO_VECTOR('[34.6, 77.8]', 2, FLOAT32) FROM dual;