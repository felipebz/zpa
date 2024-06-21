-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
DECLARE
  nt nt_type := nt_type(11, 22, 33, 44, 55, 66);
BEGIN
  print_nt(nt);

  nt.DELETE(2);     -- Delete second element
  print_nt(nt);

  nt(2) := 2222;    -- Restore second element
  print_nt(nt);

  nt.DELETE(2, 4);  -- Delete range of elements
  print_nt(nt);

  nt(3) := 3333;    -- Restore third element
  print_nt(nt);

  nt.DELETE;        -- Delete all elements
  print_nt(nt);
END;
/