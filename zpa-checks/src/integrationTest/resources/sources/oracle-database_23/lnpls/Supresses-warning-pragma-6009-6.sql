-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE OR REPLACE PACKAGE BODY pk1 IS
   PROCEDURE p1(x NUMBER) IS
   BEGIN
      DBMS_OUTPUT.PUT_LINE('In the first overload');
   END;

   PROCEDURE p1 IS
   BEGIN
      DBMS_OUTPUT.PUT_LINE('In the second overload');
      RAISE_APPLICATION_ERROR(-20000, 'Unexpected error');
   END;
END;
/