-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/assigning-values-collection-variables.html
DECLARE
  TYPE triplet IS VARRAY(3) OF VARCHAR2(15);
  TYPE trio    IS VARRAY(3) OF VARCHAR2(15);

  group1 triplet := triplet('Jones', 'Wong', 'Marceau');
  group2 triplet;
  group3 trio;
BEGIN
  group2 := group1;  -- succeeds
  group3 := group1;  -- fails
END;
/