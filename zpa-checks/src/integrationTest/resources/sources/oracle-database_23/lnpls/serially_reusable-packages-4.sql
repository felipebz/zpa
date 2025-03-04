-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/serially_reusable-packages.html
BEGIN
  fetch_from_cursor;
  fetch_from_cursor;
END;
/