-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/vector_chunks.html
CREATE TABLE documentation_tab (
  id   NUMBER,
  text VARCHAR2(2000));
INSERT INTO documentation_tab
   VALUES(1, 'sample');
COMMIT;
SET LINESIZE 100;
SET PAGESIZE 20;
COLUMN pos FORMAT 999;
COLUMN siz FORMAT 999;
COLUMN txt FORMAT a60;
SELECT D.id id, C.chunk_offset pos, C.chunk_length siz, C.chunk_text txt
FROM documentation_tab D, VECTOR_CHUNKS(D.text
                                  BY words
                                  MAX 200
                                  OVERLAP 10
                                  SPLIT BY recursively
                                  LANGUAGE american
                                  NORMALIZE all) C;