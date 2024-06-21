-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pack11 AUTHID DEFINER AS
 $IF DBMS_DB_VERSION.VER_LE_11 
 $THEN
    PROCEDURE proc1;
 $ELSE
    PROCEDURE proc1;
    PRAGMA DEPRECATE(proc1);
 $END
 PROCEDURE proc2;
 PROCEDURE proc3;
END pack11;