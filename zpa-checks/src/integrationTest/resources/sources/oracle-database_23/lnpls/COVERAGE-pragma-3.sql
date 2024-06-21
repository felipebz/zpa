-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/COVERAGE-pragma.html
CREATE OR REPLACE PROCEDURE outer IS
   PRAGMA COVERAGE ('NOT_FEASIBLE_START');
   x NUMBER := 7;
   PROCEDURE inner IS
   BEGIN
     IF x < 6 THEN
        x := 19;
     ELSE
        PRAGMA COVERAGE ('NOT_FEASIBLE'); -- 1
        x := 203;
     END IF;
   END;
BEGIN
   DBMS_OUTPUT.PUT_LINE ('X= ');
   PRAGMA COVERAGE ('NOT_FEASIBLE_END');
   DBMS_OUTPUT.PUT_LINE (x);
END;
/