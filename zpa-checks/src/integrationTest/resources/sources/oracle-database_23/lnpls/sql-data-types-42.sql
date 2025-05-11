-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
  v1 VECTOR := VECTOR('[10, 20, 30]', 3, INT8);
  v2 VECTOR := VECTOR('[6, 4, 2]', 3, INT8);
BEGIN
  DBMS_OUTPUT.PUT_LINE(TO_CHAR(v1 + v2));
  DBMS_OUTPUT.PUT_LINE(TO_CHAR(v1 - v2));
  DBMS_OUTPUT.PUT_LINE(TO_CHAR(v1 * v2));
END;  
/