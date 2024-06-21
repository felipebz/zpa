-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/GOTO-statement.html
DECLARE
  valid BOOLEAN := TRUE;
BEGIN
  GOTO update_row;

  IF valid THEN
  <<update_row>>
    NULL;
  END IF;
END;
/