-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overloaded-subprograms.html
CREATE OR REPLACE PROCEDURE p AUTHID DEFINER IS
  a pkg2.t1 := 'a';
BEGIN
  pkg2.s(p1=>a);  -- Compiles without error
END p;
/