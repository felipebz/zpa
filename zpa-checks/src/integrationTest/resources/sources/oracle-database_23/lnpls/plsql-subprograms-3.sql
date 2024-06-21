-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
DECLARE
  first_name employees.first_name%TYPE;
  last_name  employees.last_name%TYPE;
  email      employees.email%TYPE;
  employer   VARCHAR2(8) := 'AcmeCorp';

  -- Declare and define procedure

  PROCEDURE create_email (  -- Subprogram heading begins
    name1   VARCHAR2,
    name2   VARCHAR2,
    company VARCHAR2
  )                         -- Subprogram heading ends
  IS
                            -- Declarative part begins
    error_message VARCHAR2(30) := 'Email address is too long.';
  BEGIN                     -- Executable part begins
    email := name1 || '.' || name2 || '@' || company;
  EXCEPTION                      -- Exception-handling part begins
    WHEN VALUE_ERROR THEN
      DBMS_OUTPUT.PUT_LINE(error_message);
  END create_email;

BEGIN
  first_name := 'John';
  last_name  := 'Doe';

  create_email(first_name, last_name, employer);  -- invocation
  DBMS_OUTPUT.PUT_LINE ('With first name first, email is: ' || email);

  create_email(last_name, first_name, employer);  -- invocation
  DBMS_OUTPUT.PUT_LINE ('With last name first, email is: ' || email);

  first_name := 'Elizabeth';
  last_name  := 'MacDonald';
  create_email(first_name, last_name, employer);  -- invocation
END;
/