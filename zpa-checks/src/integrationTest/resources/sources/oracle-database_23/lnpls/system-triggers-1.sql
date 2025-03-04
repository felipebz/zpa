-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/system-triggers.html
CREATE TRIGGER log_errors
  AFTER SERVERERROR ON DATABASE
  BEGIN
    IF (IS_SERVERERROR (1017)) THEN
      NULL;  -- (substitute code that processes logon error)
    ELSE
      NULL;  -- (substitute code that logs error code)
    END IF;
  END;
/