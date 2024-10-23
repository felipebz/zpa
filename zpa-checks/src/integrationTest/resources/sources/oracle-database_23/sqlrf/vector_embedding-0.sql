-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_embedding.html
SELECT TO_VECTOR(VECTOR_EMBEDDING(model USING 'hello' as data)) AS embedding;