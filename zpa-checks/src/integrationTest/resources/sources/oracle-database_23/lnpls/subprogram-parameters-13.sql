-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/subprogram-parameters.html
CREATE OR REPLACE PROCEDURE p (
  a        PLS_INTEGER,  -- IN by default
  b     IN PLS_INTEGER,
  c    OUT PLS_INTEGER,
  d IN OUT BINARY_FLOAT
) AUTHID DEFINER IS
BEGIN
  -- Print values of parameters:

  DBMS_OUTPUT.PUT_LINE('Inside procedure p:');

  DBMS_OUTPUT.PUT('IN a = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(a), 'NULL'));

  DBMS_OUTPUT.PUT('IN b = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(b), 'NULL'));

  DBMS_OUTPUT.PUT('OUT c = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(c), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('IN OUT d = ' || TO_CHAR(d));

  -- Can reference IN parameters a and b,
  -- but cannot assign values to them.

  c := a+10;  -- Assign value to OUT parameter
  d := 10/b;  -- Assign value to IN OUT parameter
END;
/
DECLARE
  aa  CONSTANT PLS_INTEGER := 1;
  bb  PLS_INTEGER  := 2;
  cc  PLS_INTEGER  := 3;
  dd  BINARY_FLOAT := 4;
  ee  PLS_INTEGER;
  ff  BINARY_FLOAT := 5;
BEGIN
  DBMS_OUTPUT.PUT_LINE('Before invoking procedure p:');

  DBMS_OUTPUT.PUT('aa = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(aa), 'NULL'));

  DBMS_OUTPUT.PUT('bb = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(bb), 'NULL'));

  DBMS_OUTPUT.PUT('cc = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(cc), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('dd = ' || TO_CHAR(dd));

  p (aa, -- constant
     bb, -- initialized variable
     cc, -- initialized variable 
     dd  -- initialized variable
  );

  DBMS_OUTPUT.PUT_LINE('After invoking procedure p:');

  DBMS_OUTPUT.PUT('aa = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(aa), 'NULL'));

  DBMS_OUTPUT.PUT('bb = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(bb), 'NULL'));

  DBMS_OUTPUT.PUT('cc = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(cc), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('dd = ' || TO_CHAR(dd));

  DBMS_OUTPUT.PUT_LINE('Before invoking procedure p:');

  DBMS_OUTPUT.PUT('ee = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(ee), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('ff = ' || TO_CHAR(ff));

  p (1,        -- literal 
     (bb+3)*4, -- expression 
     ee,       -- uninitialized variable 
     ff        -- initialized variable
   );

  DBMS_OUTPUT.PUT_LINE('After invoking procedure p:');

  DBMS_OUTPUT.PUT('ee = ');
  DBMS_OUTPUT.PUT_LINE(NVL(TO_CHAR(ee), 'NULL'));

  DBMS_OUTPUT.PUT_LINE('ff = ' || TO_CHAR(ff));
END;
/