-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_dimension_count.html
SELECT VECTOR_DIMENSION_COUNT( TO_VECTOR('[34.6, 77.8]', 2, FLOAT64) );