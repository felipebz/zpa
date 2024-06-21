-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE PROCEDURE p2
AUTHID DEFINER
IS
BEGIN
  DBMS_OUTPUT.PUT_LINE('In procedure p2');
EXCEPTION
  WHEN OTHERS THEN
    p1;                
END p2;
/