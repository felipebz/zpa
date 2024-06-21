-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-packages.html
DROP TABLE people;
CREATE TABLE people (name VARCHAR2(20));
INSERT INTO people (name) VALUES ('John Smith');
INSERT INTO people (name) VALUES ('Mary Jones');
INSERT INTO people (name) VALUES ('Joe Brown');
INSERT INTO people (name) VALUES ('Jane White');
CREATE OR REPLACE PACKAGE sr_pkg IS
  PRAGMA SERIALLY_REUSABLE;
  CURSOR c IS SELECT name FROM people;
END sr_pkg;
/
CREATE OR REPLACE PROCEDURE fetch_from_cursor IS
  v_name  people.name%TYPE;
BEGIN
  IF sr_pkg.c%ISOPEN THEN
    DBMS_OUTPUT.PUT_LINE('Cursor is open.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Cursor is closed; opening now.');
    OPEN sr_pkg.c;
  END IF;

  FETCH sr_pkg.c INTO v_name;
  DBMS_OUTPUT.PUT_LINE('Fetched: ' || v_name);

  FETCH sr_pkg.c INTO v_name;
    DBMS_OUTPUT.PUT_LINE('Fetched: ' || v_name);
  END fetch_from_cursor;
/