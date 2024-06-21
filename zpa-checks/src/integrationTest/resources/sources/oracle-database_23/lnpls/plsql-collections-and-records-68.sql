-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
DECLARE
  TYPE name_rec IS RECORD (
    first  employees.first_name%TYPE,
    last   employees.last_name%TYPE
  );

  TYPE contact IS RECORD (
    name  name_rec,                    -- nested record
    phone employees.phone_number%TYPE
  );

  friend contact;
BEGIN
  friend.name.first := 'John';
  friend.name.last := 'Smith';
  friend.phone := '1-650-555-1234';

  DBMS_OUTPUT.PUT_LINE (
    friend.name.first  || ' ' ||
    friend.name.last   || ', ' ||
    friend.phone
  );
END;
/