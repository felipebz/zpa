-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-control-statements.html
BEGIN
   FOR power IN 2, REPEAT power*2 WHILE power <= 64 WHEN MOD(power, 32)= 0 LOOP
      DBMS_OUTPUT.PUT_LINE(power);
   END LOOP;
END;
/