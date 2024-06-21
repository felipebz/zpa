-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/static-sql.html
BEGIN
  CLOSE :emp_cv;
  CLOSE :dept_cv;
  CLOSE :loc_cv;
END;
/