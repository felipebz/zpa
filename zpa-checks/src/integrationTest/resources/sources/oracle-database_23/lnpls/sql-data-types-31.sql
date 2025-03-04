-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DROP TABLE theVectorTable;
CREATE TABLE theVectorTable (embedding VECTOR(3, float32), id NUMBER);
INSERT INTO theVectorTable VALUES ('[1.11, 2.22, 3.33]', 1);
INSERT INTO theVectorTable VALUES ('[4.44, 5.55, 6.66]', 2);
INSERT INTO theVectorTable VALUES ('[7.77, 8.88, 9.99]', 3);
SET SERVEROUTPUT ON;
DECLARE
  v_embedding theVectorTable.embedding%TYPE;
BEGIN
  SELECT embedding INTO v_embedding FROM theVectorTable WHERE id=3;
  DBMS_OUTPUT.PUT_LINE('Embedding is ' || FROM_VECTOR(v_embedding));
END;
/