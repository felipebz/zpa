-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/sql-data-types.html
DECLARE
  v1 VECTOR := TO_VECTOR('[1, 2, 3]');
  v2 VECTOR := TO_VECTOR('[4, 5, 6]');
  v3 VECTOR := TO_VECTOR('[1, 2, 0, 6]', *, BINARY);
  v4 VECTOR := TO_VECTOR('[0, 6, 0, 3]', *, BINARY);
  man_dist NUMBER;
  euc_dist NUMBER;
  cos_dist NUMBER;
  inn_dist NUMBER;
  ham_dist NUMBER;
  dot_dist NUMBER;
  jac_dist NUMBER;
BEGIN
  man_dist := L1_DISTANCE(v1, v2); --Manhattan Distance
  euc_dist := L2_DISTANCE(v1, v2); --Euclidean Distance
  cos_dist := COSINE_DISTANCE(v1, v2); --Cosine Distance
  inn_dist := INNER_PRODUCT(v1, v2); --Inner Product

  --The Hamming Distance has no standalone function in PL/SQL
  ham_dist := VECTOR_DISTANCE(v1, v2, HAMMING);

  --The Negative Inner (Dot) Product has no standalone function in PL/SQL
  dot_dist := VECTOR_DISTANCE(v1, v2, DOT);

  --The Jaccard Distance has no standalone function in PL/SQL
  jac_dist := VECTOR_DISTANCE(v3, v4, JACCARD);

  DBMS_OUTPUT.PUT_LINE('The Manhattan distance is: ' || man_dist);
  DBMS_OUTPUT.PUT_LINE('The Euclidean distance is: ' || euc_dist);
  DBMS_OUTPUT.PUT_LINE('The Cosine distance is: ' || cos_dist);
  DBMS_OUTPUT.PUT_LINE('The Inner Product is: ' || inn_dist);
  DBMS_OUTPUT.PUT_LINE('The Hamming distance is: ' || ham_dist);
  DBMS_OUTPUT.PUT_LINE('The Dot Product is: ' || dot_dist);
  DBMS_OUTPUT.PUT_LINE('The Jaccard Distance between the BINARY vectors v3 and v4 is: ' || jac_dist);
END;
/