-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-control-statements.html
DECLARE
  grade NUMBER;
BEGIN
  grade := '85';

  CASE grade
    WHEN < 0, > 100 THEN DBMS_OUTPUT.PUT_LINE('No such grade');
    WHEN > 89 THEN DBMS_OUTPUT.PUT_LINE('A');
    WHEN > 79 THEN DBMS_OUTPUT.PUT_LINE('B');
    WHEN > 69 THEN DBMS_OUTPUT.PUT_LINE('C');
    WHEN > 59 THEN DBMS_OUTPUT.PUT_LINE('D');
    ELSE DBMS_OUTPUT.PUT_LINE('F');
  END CASE;
END;
/