-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dynamic-sql.html
CREATE OR REPLACE PACKAGE pkg AUTHID DEFINER AS
 
  TYPE foursome IS VARRAY(4) OF VARCHAR2(5);

  PROCEDURE print_foursome (x foursome);
END pkg;
/
CREATE OR REPLACE PACKAGE BODY pkg AS
  PROCEDURE print_foursome (x foursome) IS
  BEGIN
    IF x.COUNT = 0 THEN
      DBMS_OUTPUT.PUT_LINE('Empty');
    ELSE 
      FOR i IN x.FIRST .. x.LAST LOOP
        DBMS_OUTPUT.PUT_LINE(x(i));
      END LOOP;
    END IF;
  END;
END pkg;
/
DECLARE
  directions pkg.foursome;
  dyn_stmt VARCHAR2(3000);
BEGIN
  directions := pkg.foursome('north', 'south', 'east', 'west');

  dyn_stmt := 'BEGIN pkg.print_foursome(:x); END;';
  EXECUTE IMMEDIATE dyn_stmt USING directions;
END;
/