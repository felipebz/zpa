CREATE PACKAGE p AS
  TYPE rec_t IS RECORD (
    id NUMBER,
    label VARCHAR2(40)
  );

  TYPE rows_t IS TABLE OF rec_t INDEX BY PLS_INTEGER;
  TYPE small_rows_t IS VARRAY(5) OF rec_t;
END p;
/
CREATE TYPE ids AS TABLE OF NUMBER;
/
CREATE TYPE foreign_rows AS TABLE OF app.rec_t;
/
