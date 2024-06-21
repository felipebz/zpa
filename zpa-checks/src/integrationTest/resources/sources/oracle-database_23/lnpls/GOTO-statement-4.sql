-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/GOTO-statement.html
DECLARE
  done  BOOLEAN;
BEGIN
  FOR i IN 1..50 LOOP
    IF done THEN
      GOTO end_loop;
    END IF;
    <<end_loop>>
    NULL;
  END LOOP;
END;
/