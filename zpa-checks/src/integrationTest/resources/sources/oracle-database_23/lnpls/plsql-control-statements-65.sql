-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-control-statements.html
BEGIN
   FOR power IN 1, REPEAT power*2 WHILE power <= 64 LOOP
      DBMS_OUTPUT.PUT_LINE(power);
   END LOOP;
END;
/