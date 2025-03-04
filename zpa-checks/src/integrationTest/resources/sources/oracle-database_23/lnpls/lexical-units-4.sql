-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/lexical-units.html
DECLARE
  "HELLO" varchar2(10) := 'hello';
BEGIN
  DBMS_Output.Put_Line(Hello);
END;
/