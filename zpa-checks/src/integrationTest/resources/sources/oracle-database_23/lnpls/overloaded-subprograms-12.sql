-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overloaded-subprograms.html
CREATE OR REPLACE PACKAGE pack1 AUTHID DEFINER AS
  PROCEDURE proc1 (a NUMBER, b VARCHAR2);
  PROCEDURE proc1 (a NUMBER, b NUMBER);
END;
/
CREATE OR REPLACE PACKAGE BODY pack1 AS
  PROCEDURE proc1 (a NUMBER, b VARCHAR2) IS BEGIN NULL; END;
  PROCEDURE proc1 (a NUMBER, b NUMBER) IS BEGIN NULL; END;
END;
/
BEGIN
  pack1.proc1(1,'2');    -- Compiles without error
  pack1.proc1(1,2);      -- Compiles without error
  pack1.proc1('1','2');  -- Causes compile-time error PLS-00307
  pack1.proc1('1',2);    -- Causes compile-time error PLS-00307
END;
/