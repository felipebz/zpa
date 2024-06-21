-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dynamic-sql.html
CREATE OR REPLACE PACKAGE pkg AUTHID DEFINER AS
 
  TYPE names IS TABLE OF VARCHAR2(10);

  PROCEDURE print_names (x names);
END pkg;
/
CREATE OR REPLACE PACKAGE BODY pkg AS
  PROCEDURE print_names (x names) IS
  BEGIN
    FOR i IN x.FIRST .. x.LAST LOOP
      DBMS_OUTPUT.PUT_LINE(x(i));
    END LOOP;
  END;
END pkg;
/
DECLARE
  fruits   pkg.names;
  dyn_stmt VARCHAR2(3000);
BEGIN
  fruits := pkg.names('apple', 'banana', 'cherry');

  dyn_stmt := 'BEGIN pkg.print_names(:x); END;';
  EXECUTE IMMEDIATE dyn_stmt USING fruits;
END;
/