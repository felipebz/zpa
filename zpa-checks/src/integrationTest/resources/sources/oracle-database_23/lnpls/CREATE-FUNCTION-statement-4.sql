-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-FUNCTION-statement.html
CREATE OR REPLACE FUNCTION text_length(a CLOB) 
   RETURN NUMBER DETERMINISTIC IS
BEGIN 
  RETURN DBMS_LOB.GETLENGTH(a);
END;