-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
   BEGIN
     DBMS_OUTPUT.PUT_LINE('Executing inner_foo');
   END;
BEGIN
  DBMS_OUTPUT.PUT_LINE('Executing foo');
END;