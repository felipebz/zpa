CREATE PACKAGE p AS
  SUBTYPE money IS NUMBER;
  SUBTYPE amount IS money;

  TYPE inner_t IS RECORD (
    n amount
  );

  TYPE outer_t IS RECORD (
    child inner_t
  );

  TYPE stop_t IS RECORD (
    ext other.ext_t,
    anch emp.sal%TYPE,
    row_anch emp%ROWTYPE,
    obj REF obj_t
  );

  SUBTYPE c1 IS c2;
  SUBTYPE c2 IS c1;
END p;
/
