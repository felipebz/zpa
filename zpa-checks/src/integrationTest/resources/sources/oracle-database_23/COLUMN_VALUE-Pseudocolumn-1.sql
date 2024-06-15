-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/COLUMN_VALUE-Pseudocolumn.html
CREATE TYPE phone AS TABLE OF NUMBER;   
/
CREATE TYPE phone_list AS TABLE OF phone;
/