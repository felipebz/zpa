-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_serialize.html
SELECT VECTOR_SERIALIZE(VECTOR('[1.1, 2.2, 3.3]',3,FLOAT32) RETURNING CLOB);
