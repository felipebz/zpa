-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parameters.html
BEGIN
  print_name('John', 'Doe');          -- original invocation
  print_name('John', 'Public', 'Q');  -- new invocation
END;
/