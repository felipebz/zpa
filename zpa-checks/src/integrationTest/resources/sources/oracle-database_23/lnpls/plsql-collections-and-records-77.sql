-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
CREATE OR REPLACE PROCEDURE print (n INTEGER) IS 
BEGIN 
  IF n IS NOT NULL THEN 
    DBMS_OUTPUT.PUT_LINE(n); 
  ELSE 
    DBMS_OUTPUT.PUT_LINE('NULL'); 
  END IF; 
END print; 
/
DROP TABLE t1;
CREATE TABLE t1 (
  c1 INTEGER DEFAULT 0 NOT NULL,
  c2 INTEGER DEFAULT 1 NOT NULL
);
DECLARE
  t1_row t1%ROWTYPE;
BEGIN
  DBMS_OUTPUT.PUT('t1.c1 = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(t1_row.c1), 'NULL'));

  DBMS_OUTPUT.PUT('t1.c2 = '); print(t1_row.c2);
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(t1_row.c2), 'NULL'));
END;
/