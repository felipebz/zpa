-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/from_vector.html
SELECT FROM_VECTOR(TO_VECTOR('[1.1, 2.2, 3.3]', 3, FLOAT32) RETURNING CLOB );