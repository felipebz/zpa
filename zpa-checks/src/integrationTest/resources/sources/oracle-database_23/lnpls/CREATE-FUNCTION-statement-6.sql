-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-FUNCTION-statement.html
BEGIN
  DBMS_OUTPUT.PUT_LINE(hello_inline('Jane'));
END;
/