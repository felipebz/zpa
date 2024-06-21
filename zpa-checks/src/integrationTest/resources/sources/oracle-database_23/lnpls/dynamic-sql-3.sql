-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/dynamic-sql.html
CREATE OR REPLACE PACKAGE pkg AUTHID DEFINER AS
 
  TYPE rec IS RECORD (n1 NUMBER, n2 NUMBER);

  PROCEDURE p (x OUT rec, y NUMBER, z NUMBER);
END pkg;
/
CREATE OR REPLACE PACKAGE BODY pkg AS
 
  PROCEDURE p (x OUT rec, y NUMBER, z NUMBER) AS
  BEGIN
    x.n1 := y;
    x.n2 := z;
  END p;
END pkg;
/
DECLARE
  r       pkg.rec;
  dyn_str VARCHAR2(3000);
BEGIN
  dyn_str := 'BEGIN pkg.p(:x, 6, 8); END;';

  EXECUTE IMMEDIATE dyn_str USING OUT r;

  DBMS_OUTPUT.PUT_LINE('r.n1 = ' || r.n1);
  DBMS_OUTPUT.PUT_LINE('r.n2 = ' || r.n2);
END;
/