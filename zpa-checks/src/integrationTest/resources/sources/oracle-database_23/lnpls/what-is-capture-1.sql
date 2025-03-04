-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/what-is-capture.html
CREATE OR REPLACE PROCEDURE proc AUTHID DEFINER AS
  CURSOR c1 IS
    SELECT * FROM tab1
    WHERE EXISTS (SELECT * FROM tab2 WHERE col2 = 10);
BEGIN
  OPEN c1;
  CLOSE c1;
END;
/