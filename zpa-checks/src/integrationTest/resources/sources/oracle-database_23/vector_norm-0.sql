-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_norm.html
SELECT VECTOR_NORM( TO_VECTOR('[4, 3]', 2, FLOAT32) );

VECTOR_NORM(TO_VECTOR('[4,3]',2,FLOAT32))
____________________________________________
5.0