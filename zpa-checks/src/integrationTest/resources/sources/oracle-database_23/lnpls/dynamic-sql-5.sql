-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dynamic-sql.html
CREATE OR REPLACE PACKAGE pkg AUTHID DEFINER AS
 
  TYPE number_names IS TABLE OF VARCHAR2(5)
    INDEX BY PLS_INTEGER;

  PROCEDURE print_number_names (x number_names);
END pkg;
/
CREATE OR REPLACE PACKAGE BODY pkg AS
  PROCEDURE print_number_names (x number_names) IS
  BEGIN
    FOR i IN x.FIRST .. x.LAST LOOP
      DBMS_OUTPUT.PUT_LINE(x(i));
    END LOOP;
  END;
END pkg;
/
DECLARE  
  digit_names  pkg.number_names;
  dyn_stmt     VARCHAR2(3000);
BEGIN
  digit_names(0) := 'zero';
  digit_names(1) := 'one';
  digit_names(2) := 'two';
  digit_names(3) := 'three';
  digit_names(4) := 'four';
  digit_names(5) := 'five';
  digit_names(6) := 'six';
  digit_names(7) := 'seven';
  digit_names(8) := 'eight';
  digit_names(9) := 'nine';

  dyn_stmt := 'BEGIN pkg.print_number_names(:x); END;';
  EXECUTE IMMEDIATE dyn_stmt USING digit_names;
END;
/