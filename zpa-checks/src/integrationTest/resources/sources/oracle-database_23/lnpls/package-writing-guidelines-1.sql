-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/package-writing-guidelines.html
CREATE OR REPLACE PACKAGE helper
  AUTHID DEFINER
  ACCESSIBLE BY (api)
IS
  PROCEDURE h1;
  PROCEDURE h2;
END;
/
CREATE OR REPLACE PACKAGE BODY helper
IS
  PROCEDURE h1 IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('Helper procedure h1');
  END;

  PROCEDURE h2 IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('Helper procedure h2');
  END;
END;
/
CREATE OR REPLACE PACKAGE api
  AUTHID DEFINER
IS
  PROCEDURE p1;
  PROCEDURE p2;
END;
/
CREATE OR REPLACE PACKAGE BODY api
IS
  PROCEDURE p1 IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('API procedure p1');
    helper.h1;
  END;

  PROCEDURE p2 IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('API procedure p2');
    helper.h2;
  END;
END;
/