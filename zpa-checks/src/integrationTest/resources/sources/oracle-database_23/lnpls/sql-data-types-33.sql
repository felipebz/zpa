-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
  TYPE vecTabT IS TABLE OF theVectorTable%ROWTYPE INDEX BY BINARY_INTEGER;
  v_vecTabT vecTabT;
  CURSOR c IS SELECT * FROM theVectorTable;
BEGIN
  OPEN c;
  FETCH c BULK COLLECT INTO v_vecTabT;
  CLOSE c;

  -- display the contents of the vector index table
  FOR i IN 1..v_vecTabT.LAST LOOP
    DBMS_OUTPUT.PUT_LINE('Embedding ID ' || v_vecTabT(i).id || ': ' ||
            FROM_VECTOR(v_vecTabT(i).embedding));
  END LOOP;
END;
/