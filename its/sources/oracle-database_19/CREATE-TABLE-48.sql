-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-TABLE.html
CREATE TYPE department_typ AS OBJECT
   ( d_name   VARCHAR2(100), 
     d_address VARCHAR2(200) );
/