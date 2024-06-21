-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE PROCEDURE p1
AUTHID DEFINER
IS
    PRAGMA SUPPRESSES_WARNING_6009(p1);
BEGIN
    RAISE_APPLICATION_ERROR(-20000, 'Unexpected error raised');
END;
/