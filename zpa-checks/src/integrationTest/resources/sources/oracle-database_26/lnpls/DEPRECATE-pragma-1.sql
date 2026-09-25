-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pack1 AS
PRAGMA DEPRECATE(pack1);
 PROCEDURE foo;
 PROCEDURE bar;
END pack1;