-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
BEGIN
   FOR i IN 1, REPEAT i*2 WHILE i < 100 LOOP
      DBMS_OUTPUT.PUT_LINE(i);
   END LOOP;
END;
/