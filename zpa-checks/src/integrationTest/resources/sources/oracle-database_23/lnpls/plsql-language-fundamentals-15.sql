-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-language-fundamentals.html
BEGIN
  DBMS_OUTPUT.PUT_LINE('This string breaks
here.');
END;
/