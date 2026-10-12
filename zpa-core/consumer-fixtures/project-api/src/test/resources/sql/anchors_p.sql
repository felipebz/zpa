CREATE PACKAGE p AS
  SUBTYPE numeric_t IS NUMBER;

  g NUMBER;
  c CONSTANT VARCHAR2(30) := 'value';

  first_value numeric_t;
  second_value first_value%TYPE;
  third_value second_value%TYPE;

  TYPE rec_t IS RECORD (id NUMBER);
  TYPE rows_t IS TABLE OF rec_t;

  rec_value rec_t;
  rows_value rows_t;

  field_ref rec_value.id%TYPE;
  later_ref later_value%TYPE;
  later_value NUMBER;
  row_ref employees%ROWTYPE;
  unknown_value unknown_t;
  unknown_ref unknown_value%TYPE;

  TYPE cap_t IS RECORD (a NUMBER, b a%TYPE);
END p;
/
