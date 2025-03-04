-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parts.html
DECLARE
  PROCEDURE p IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('Inside p');
    RETURN;
    DBMS_OUTPUT.PUT_LINE('Unreachable statement.');
  END;
BEGIN
  p;
  DBMS_OUTPUT.PUT_LINE('Control returns here.');
END;
/