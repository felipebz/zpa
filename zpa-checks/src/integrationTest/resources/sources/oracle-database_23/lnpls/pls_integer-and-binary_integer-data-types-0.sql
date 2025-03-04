-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pls_integer-and-binary_integer-data-types.html
DECLARE
  p1 PLS_INTEGER := 2147483647;
  p2 PLS_INTEGER := 1;
  n NUMBER;
BEGIN
  n := p1 + p2;
END;
/