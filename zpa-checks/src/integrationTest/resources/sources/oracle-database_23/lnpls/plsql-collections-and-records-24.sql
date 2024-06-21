-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-collections-and-records.html
DECLARE
  TYPE nested_typ IS TABLE OF NUMBER;
  nt1 nested_typ := nested_typ(1,2,3);
  nt2 nested_typ := nested_typ(3,2,1);
  nt3 nested_typ := nested_typ(2,3,1,3);
  nt4 nested_typ := nested_typ(1,2,4);

  PROCEDURE testify (
    truth BOOLEAN := NULL,
    quantity NUMBER := NULL
  ) IS
  BEGIN
    IF truth IS NOT NULL THEN
      DBMS_OUTPUT.PUT_LINE (
        CASE truth
           WHEN TRUE THEN 'True'
           WHEN FALSE THEN 'False'
        END
      );
    END IF;
    IF quantity IS NOT NULL THEN
        DBMS_OUTPUT.PUT_LINE(quantity);
    END IF;
  END;
BEGIN
  testify(truth => (nt1 IN (nt2,nt3,nt4)));        -- condition
  testify(truth => (nt1 SUBMULTISET OF nt3));      -- condition
  testify(truth => (nt1 NOT SUBMULTISET OF nt4));  -- condition
  testify(truth => (4 MEMBER OF nt1));             -- condition
  testify(truth => (nt3 IS A SET));                -- condition
  testify(truth => (nt3 IS NOT A SET));            -- condition
  testify(truth => (nt1 IS EMPTY));                -- condition
  testify(quantity => (CARDINALITY(nt3)));         -- function
  testify(quantity => (CARDINALITY(SET(nt3))));    -- 2 functions
END;
/