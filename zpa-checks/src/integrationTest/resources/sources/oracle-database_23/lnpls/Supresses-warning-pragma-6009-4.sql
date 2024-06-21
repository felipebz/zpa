-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE FUNCTION f2(numval NUMBER) RETURN NUMBER
AUTHID DEFINER
IS
  i NUMBER;
BEGIN
  i := numval + 1;
  RETURN i;
EXCEPTION
  WHEN OTHERS THEN
    RETURN f1(i);
END;
/