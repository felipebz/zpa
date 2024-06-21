-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE OR REPLACE PACKAGE BODY pk2 IS
   PROCEDURE pn; /* Forward declaration */
   PRAGMA SUPPRESSES_WARNING_6009(pn);

   PROCEDURE p5 IS
   BEGIN
      DBMS_OUTPUT.PUT_LINE('Computing');
   EXCEPTION 
      WHEN OTHERS THEN
         pn;
   END;

   PROCEDURE pn IS
   BEGIN
      RAISE_APPLICATION_ERROR(-20000, 'Unexpected error');
   END;
END;
/