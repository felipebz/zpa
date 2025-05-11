-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
  vs1 VECTOR(*, *, SPARSE) := VECTOR('[10, [0, 3, 4, 6, 7], [1.9, 4, 7.2, 30, 60]]', *, *, SPARSE);
  vs2 VECTOR(*, *, SPARSE) := VECTOR('[10, [1, 3, 4, 8, 9], [4.5, 7.6, 4, 8.1, 5]]', *, *, SPARSE);

  vd1 VECTOR(*, *, DENSE) := VECTOR('[1.9, 0, 0, 4, 7.2, 0, 30, 60, 0, 0]', *, *, DENSE);
  vd2 VECTOR(*, *, DENSE) := VECTOR('[0, 4.5, 0, 7.6, 4, 0, 0, 0, 8.1, 5]', *, *, DENSE);
BEGIN
  DBMS_OUTPUT.PUT_LINE('Vector Distance Sparse: ' || TRUNC(VECTOR_DISTANCE(vs1, vs2), 5));
  DBMS_OUTPUT.PUT_LINE('Vector Distance Dense:  ' || TRUNC(VECTOR_DISTANCE(vd1, vd2), 5));
END;
/