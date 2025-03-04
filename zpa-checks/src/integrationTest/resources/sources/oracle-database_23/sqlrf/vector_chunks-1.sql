-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_chunks.html
COLUMN chunk_offset HEADING Offset FORMAT 999

COLUMN chunk_length HEADING Len    FORMAT 999

COLUMN chunk_text   HEADING Text   FORMAT a60

VARIABLE txt VARCHAR2(4000)

EXECUTE :txt := 'An example text value to split with VECTOR_CHUNKS, having over 10 words because the minimum MAX value is 10';
SELECT * FROM VECTOR_CHUNKS(:txt BY WORDS MAX 10);
SELECT * FROM VECTOR_CHUNKS('Another example text value to split with VECTOR_CHUNKS, having over 10 words because the minimum MAX value is 10' BY WORDS MAX 10);