-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-DATABASE-LINK.html
CREATE DATABASE LINK local 
   CONNECT TO hr IDENTIFIED BY password
   USING 'local';