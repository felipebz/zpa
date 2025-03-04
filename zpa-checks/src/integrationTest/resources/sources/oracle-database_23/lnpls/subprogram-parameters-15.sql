-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parameters.html
DECLARE
  j  PLS_INTEGER  := 10;
  k  BINARY_FLOAT := 15;
BEGIN
  DBMS_OUTPUT.PUT_LINE('Before invoking procedure p:');

  DBMS_OUTPUT.PUT('j = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(j), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('k = ' || TO_CHAR(k));

  p(4, 0, j, k);  -- causes p to exit with exception ZERO_DIVIDE

EXCEPTION
  WHEN ZERO_DIVIDE THEN
    DBMS_OUTPUT.PUT_LINE('After invoking procedure p:');

    DBMS_OUTPUT.PUT('j = ');
    DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(j), 'NULL'));

    DBMS_OUTPUT.PUT_LINE('k = ' || TO_CHAR(k));
END;
/