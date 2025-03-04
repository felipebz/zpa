-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/recursive-subprograms.html
CREATE OR REPLACE FUNCTION factorial (
  n POSITIVE
) RETURN POSITIVE
  AUTHID DEFINER
IS
BEGIN
  IF n = 1 THEN                 -- terminating condition
    RETURN n;
  ELSE
    RETURN n * factorial(n-1);  -- recursive invocation
  END IF;
END;
/
BEGIN
  FOR i IN 1..5 LOOP
    DBMS_OUTPUT.PUT_LINE(i || '! = ' || factorial(i));
  END LOOP;
END;
/