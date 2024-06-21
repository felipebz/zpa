-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE FUNCTION f1(id NUMBER) RETURN NUMBER
AUTHID DEFINER
IS
  PRAGMA SUPPRESSES_WARNING_6009(f1);
  x NUMBER;
BEGIN
  x := id + 1;
RETURN x;
END;
/