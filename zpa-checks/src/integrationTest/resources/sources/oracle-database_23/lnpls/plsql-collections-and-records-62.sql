-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
CREATE OR REPLACE PACKAGE My_Types AUTHID CURRENT_USER IS
  TYPE My_Rec IS RECORD (a NUMBER, b NUMBER);
  FUNCTION Init_My_Rec RETURN My_Rec;
END My_Types;
/
CREATE OR REPLACE PACKAGE BODY My_Types IS
  FUNCTION Init_My_Rec RETURN My_Rec IS
    Rec My_Rec;
  BEGIN
    Rec.a := 0;
    Rec.b := 1;
    RETURN Rec;
  END Init_My_Rec;
END My_Types;
/
DECLARE
  r CONSTANT My_Types.My_Rec := My_Types.Init_My_Rec();
BEGIN
  DBMS_OUTPUT.PUT_LINE('r.a = ' || r.a);
  DBMS_OUTPUT.PUT_LINE('r.b = ' || r.b);
END;
/