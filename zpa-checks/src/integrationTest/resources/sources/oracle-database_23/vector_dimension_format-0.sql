-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_dimension_format.html
SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8]', 2, FLOAT64));
FLOAT64

SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8, 9]', 3, FLOAT32));
FLOAT32

SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8, 9, 10]', 3, INT8));
SELECT VECTOR_DIMENSION_FORMAT(TO_VECTOR('[34.6, 77.8, 9, 10]', 3, INT8))                           
                                                                        *
ERROR at line 1:
ORA-51803: Vector dimension count must match the dimension count specified inthe column definition (actual: 4, required: 3).
