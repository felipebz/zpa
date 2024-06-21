-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
CREATE OR REPLACE TYPE NumList IS TABLE OF NUMBER;
  -- standalone collection type identical to package type
/
DECLARE
  n1 pkg.NumList := pkg.NumList(2,4);  -- package type
  n2     NumList :=     NumList(6,8);  -- standalone type

BEGIN
  pkg.print_numlist(n1);  -- succeeds
  pkg.print_numlist(n2);  -- fails
END;
/