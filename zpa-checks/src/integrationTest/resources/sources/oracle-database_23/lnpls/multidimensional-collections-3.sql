-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/multidimensional-collections.html
DECLARE
  TYPE tb1 IS TABLE OF INTEGER INDEX BY PLS_INTEGER;  -- associative arrays
  v4 tb1;
  v5 tb1;

  TYPE aa1 IS TABLE OF tb1 INDEX BY PLS_INTEGER;  -- associative array of
  v2 aa1;                                         --  associative arrays

  TYPE va1 IS VARRAY(10) OF VARCHAR2(20);  -- varray of strings
  v1 va1 := va1('hello', 'world');

  TYPE ntb2 IS TABLE OF va1 INDEX BY PLS_INTEGER;  -- associative array of varrays
  v3 ntb2;

BEGIN
  v4(1)   := 34;     -- populate associative array
  v4(2)   := 46456;
  v4(456) := 343;

  v2(23) := v4;  -- populate associative array of associative arrays

  v3(34) := va1(33, 456, 656, 343);  -- populate associative array varrays

  v2(35) := v5;      -- assign empty associative array to v2(35)
  v2(35)(2) := 78;
END;
/