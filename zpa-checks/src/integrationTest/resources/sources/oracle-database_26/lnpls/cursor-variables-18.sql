-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/cursor-variables.html
BEGIN
  CLOSE :emp_cv;
  CLOSE :dept_cv;
  CLOSE :loc_cv;
END;
/