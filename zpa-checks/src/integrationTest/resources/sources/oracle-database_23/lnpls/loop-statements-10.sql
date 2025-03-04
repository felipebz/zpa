-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/loop-statements.html
DECLARE
   i PLS_INTEGER;
BEGIN
   FOR i IN 1..3, REVERSE i+1..i+10, 51..55 LOOP
      DBMS_OUTPUT.PUT_LINE(i);
   END LOOP;
END;
/