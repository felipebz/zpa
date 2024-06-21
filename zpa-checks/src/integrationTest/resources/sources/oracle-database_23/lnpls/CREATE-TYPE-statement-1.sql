-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/CREATE-TYPE-statement.html
CREATE TYPE data_typ1 AS OBJECT 
   ( year NUMBER, 
     MEMBER FUNCTION prod(invent NUMBER) RETURN NUMBER 
   ); 
/
CREATE TYPE BODY data_typ1 IS   
      MEMBER FUNCTION prod (invent NUMBER) RETURN NUMBER IS 
         BEGIN 
             RETURN (year + invent);
         END; 
      END;
/