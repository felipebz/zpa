-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pack2 AS
  PROCEDURE proc1(n1 NUMBER, n2 NUMBER, n3 NUMBER);
  -- Only the overloaded procedure with 2 arguments is deprecated
  PROCEDURE proc1(n1 NUMBER, n2 NUMBER);
      PRAGMA DEPRECATE(proc1);
 END pack2;