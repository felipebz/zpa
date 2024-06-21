-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE OR REPLACE PROCEDURE p6 AUTHID DEFINER IS
j NUMBER := 5;
BEGIN
   j := j + 2;
EXCEPTION
   WHEN OTHERS THEN
       pk1.p1;
END;
/