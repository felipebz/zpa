-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-control-statements.html
BEGIN
   FOR i IN 1 LOOP
      DBMS_OUTPUT.PUT_LINE(i);
   END LOOP;
END;
/