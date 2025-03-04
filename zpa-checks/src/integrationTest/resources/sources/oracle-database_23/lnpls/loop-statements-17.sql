-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
BEGIN
   FOR n NUMBER(5,1) IN 1.0 .. 3.0 BY 0.5 LOOP
      DBMS_OUTPUT.PUT_LINE(n);
   END LOOP;
END;
/