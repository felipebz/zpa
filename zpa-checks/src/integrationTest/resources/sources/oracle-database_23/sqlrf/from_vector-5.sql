-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/from_vector.html
SELECT FROM_VECTOR(TO_VECTOR('[5,[2,4],[1.0,2.0]]', 5, FLOAT64, SPARSE) RETURNING CLOB FORMAT DENSE);