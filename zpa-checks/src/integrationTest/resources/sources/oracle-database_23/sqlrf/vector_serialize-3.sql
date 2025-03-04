-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_serialize.html
SELECT VECTOR_SERIALIZE(TO_VECTOR('[5,[2,4],[1.0,2.0]]', 5, FLOAT64, SPARSE) RETURNING CLOB FORMAT SPARSE);