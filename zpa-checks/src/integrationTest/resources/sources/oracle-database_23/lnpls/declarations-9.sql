-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/declarations.html
DECLARE
  surname  employees.last_name%TYPE;
BEGIN
  DBMS_OUTPUT.PUT_LINE('surname=' || surname);
END;
/