-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/assigning-values-record-variables.html
DECLARE
  TYPE name_rec IS RECORD (
    first  employees.first_name%TYPE DEFAULT 'John',
    last   employees.last_name%TYPE DEFAULT 'Doe'
  );

  CURSOR c IS
    SELECT first_name, last_name
    FROM employees;

  target name_rec;
  source c%ROWTYPE;

BEGIN
  source.first_name := 'Jane'; source.last_name := 'Smith';

  DBMS_OUTPUT.PUT_LINE (
    'source: ' || source.first_name || ' ' || source.last_name
  );

 target := source;

 DBMS_OUTPUT.PUT_LINE (
   'target: ' || target.first || ' ' || target.last
 );
END;
/