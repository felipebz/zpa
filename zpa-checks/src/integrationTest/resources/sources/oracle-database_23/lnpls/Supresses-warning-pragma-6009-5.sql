-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE PACKAGE pk1 IS
   PROCEDURE p1(x NUMBER);
   PROCEDURE p1;
   PRAGMA SUPPRESSES_WARNING_6009(p1);
END;
/