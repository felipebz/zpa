-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQLERRM-function.html
BEGIN
  DBMS_OUTPUT.PUT_LINE('SQLERRM(-6511): ' || TO_CHAR(SQLERRM(-6511)));
END;
/