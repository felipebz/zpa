-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/cursor-variables.html
BEGIN
  OPEN :c1 FOR SELECT 1 FROM DUAL;
  OPEN :c2 FOR SELECT 1 FROM DUAL;
  OPEN :c3 FOR SELECT 1 FROM DUAL;
END;
/