-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pkg13
AS
  PRAGMA DEPRECATE ('pkg13', 'Package pkg13 is deprecated, use pkg03');
  Y NUMBER;
END pkg13;