-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
BEGIN
   PRAGMA DEPRECATE(bar);
   DBMS_OUTPUT.PUT_LINE('Executing bar.');
END;