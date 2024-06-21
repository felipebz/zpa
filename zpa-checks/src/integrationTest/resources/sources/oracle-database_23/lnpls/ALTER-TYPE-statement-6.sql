-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/ALTER-TYPE-statement.html
CREATE TYPE link1 AS OBJECT
  (a NUMBER); 
/
CREATE TYPE link2 AS OBJECT
  (a NUMBER, 
   b link1, 
   MEMBER FUNCTION p(c1 NUMBER) RETURN NUMBER); 
/
CREATE TYPE BODY link2 AS
   MEMBER FUNCTION p(c1 NUMBER) RETURN NUMBER IS 
      BEGIN  
         dbms_output.put_line(c1);
         RETURN c1; 
      END; 
   END; 
/