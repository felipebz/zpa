-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dml-triggers.html
INSERT INTO Target
  SELECT c1, c2, c3
  FROM Source
  WHERE Source.c1 > 0