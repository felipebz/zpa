-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/exception-propagation.html
CREATE OR REPLACE PROCEDURE print_reciprocal (n NUMBER) AUTHID DEFINER IS
BEGIN

  BEGIN
    DBMS_OUTPUT.PUT_LINE(1/n);
  EXCEPTION
    WHEN ZERO_DIVIDE THEN
      DBMS_OUTPUT.PUT_LINE('Error in inner block:');
      DBMS_OUTPUT.PUT_LINE(1/n || ' is undefined.');
  END;

EXCEPTION
  WHEN ZERO_DIVIDE THEN  -- handles exception raised in exception handler
    DBMS_OUTPUT.PUT('Error in outer block: ');
    DBMS_OUTPUT.PUT_LINE('1/0 is undefined.');
END;
/
BEGIN
  print_reciprocal(0);
END;
/