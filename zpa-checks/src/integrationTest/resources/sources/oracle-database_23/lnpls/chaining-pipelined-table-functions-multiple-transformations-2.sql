-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/chaining-pipelined-table-functions-multiple-transformations.html
CREATE OR REPLACE PACKAGE pkg1 AUTHID DEFINER AS
  TYPE numset_t IS TABLE OF NUMBER;
  FUNCTION f1(x NUMBER) RETURN numset_t PIPELINED;
END pkg1;
/