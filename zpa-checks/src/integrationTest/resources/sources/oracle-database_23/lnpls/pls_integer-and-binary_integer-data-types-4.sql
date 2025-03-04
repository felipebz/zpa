-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pls_integer-and-binary_integer-data-types.html
DECLARE
  a SIMPLE_INTEGER := 1;
  b PLS_INTEGER := NULL;
BEGIN
  a := b;
END;
/