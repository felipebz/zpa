-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_dimension_format.html
SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8]', 2, FLOAT64));
SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8, 9]', 3, FLOAT32));
SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8, 9, 10]', 3, INT8));
SELECT TO_VECTOR('[34.6, 77.8, 9.10]', 3, INT8);