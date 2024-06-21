-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/Supresses-warning-pragma-6009.html
CREATE OR REPLACE TYPE BODY newid
AS
  MEMBER PROCEDURE incr
  IS
  BEGIN
     DBMS_OUTPUT.PUT_LINE('Computing value');
  EXCEPTION
     WHEN OTHERS THEN
        log_error;
  END;

  MEMBER PROCEDURE log_error
  IS
  BEGIN
    RAISE_APPLICATION_ERROR(-20000, 'Unexpected error');
  END;
END;
/