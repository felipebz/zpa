-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/SQLERRM-function.html
BEGIN
  DBMS_OUTPUT.PUT_LINE('SQLERRM(-50000): ' || TO_CHAR(SQLERRM(-50000)));
END;
/