-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-data-types.html
DECLARE
  t_b boolean := TRUE;
  f_b boolean := FALSE;
BEGIN
  DBMS_OUTPUT.PUT_LINE('My bool is: ' || t_b);
  DBMS_OUTPUT.PUT_LINE('My bool is: ' || f_b);
END;