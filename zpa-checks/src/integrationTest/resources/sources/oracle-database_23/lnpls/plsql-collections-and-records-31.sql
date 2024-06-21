-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
DECLARE
  nt nt_type := nt_type(11, 22, 33, 44, 55, 66);
BEGIN
  print_nt(nt);

  nt.TRIM;       -- Trim last element
  print_nt(nt);

  nt.DELETE(4);  -- Delete fourth element
  print_nt(nt);

  nt.TRIM(2);    -- Trim last two elements
  print_nt(nt);
END;
/