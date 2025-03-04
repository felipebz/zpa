-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/record-variables.html
DECLARE
  TYPE t_size IS RECORD (x NUMBER, y NUMBER);
  c_small  CONSTANT t_size := t_size(32,36);
  c_large  CONSTANT t_size := t_size(x => 192, y => 292);
BEGIN
  DBMS_OUTPUT.PUT_LINE('Small size is ' || c_small.x  || ' by ' || c_small.y);
  DBMS_OUTPUT.PUT_LINE('Large size is ' || c_large.x  || ' by ' || c_large.y);
END;
/