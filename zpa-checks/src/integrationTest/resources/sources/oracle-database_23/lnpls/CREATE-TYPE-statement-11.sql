-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE TYPE demo_typ2 AS OBJECT (a1 NUMBER, 
   MEMBER FUNCTION get_square RETURN NUMBER);
/