-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/overloaded-subprograms.html
CREATE OR REPLACE PACKAGE pkg3 AUTHID DEFINER IS
  PROCEDURE s (p1 VARCHAR2);
  PROCEDURE s (p1 VARCHAR2, p2 VARCHAR2 := 'p2');
END pkg3;
/