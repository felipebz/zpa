-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-subprograms.html
DECLARE
  global PLS_INTEGER := 0;

  FUNCTION f RETURN PLS_INTEGER IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE('Inside f.');
    global := global + 1;
    RETURN global * 2;
  END f;

  PROCEDURE p (
    x IN PLS_INTEGER := f()
  ) IS
  BEGIN  
    DBMS_OUTPUT.PUT_LINE (
      'Inside p. ' || 
      '  global = ' || global ||
      ', x = ' || x || '.'
    );
    DBMS_OUTPUT.PUT_LINE('--------------------------------');
  END p;

  PROCEDURE pre_p IS
  BEGIN
    DBMS_OUTPUT.PUT_LINE (
     'Before invoking p,  global = ' || global || '.'
    );
    DBMS_OUTPUT.PUT_LINE('Invoking p.');
  END pre_p;

BEGIN
  pre_p;
  p();     -- default expression is evaluated

  pre_p;
  p(100);  -- default expression is not evaluated

  pre_p;
  p();     -- default expression is evaluated
END;
/