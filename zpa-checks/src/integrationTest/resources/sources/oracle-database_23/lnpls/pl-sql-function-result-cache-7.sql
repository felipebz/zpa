-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/pl-sql-function-result-cache.html
CREATE OR REPLACE FUNCTION fibonacci (n NUMBER)
  RETURN NUMBER
  RESULT_CACHE
  AUTHID DEFINER
IS
BEGIN
  IF (n =0) OR (n =1) THEN
    RETURN 1;
  ELSE
    RETURN fibonacci(n - 1) + fibonacci(n - 2);
  END IF;
END;
/